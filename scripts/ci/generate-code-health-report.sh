#!/usr/bin/env sh
set -eu

output_dir="${1:-docs/reports}"
script_path="$0"
case "$script_path" in
  */*) script_dir="${script_path%/*}" ;;
  *) script_dir="." ;;
esac
repo_root="$(cd "$script_dir/../.." && pwd)"
resolved_output_dir="$repo_root/$output_dir"
today_stamp="$(date +"%Y%m%d")"
latest_report="$resolved_output_dir/code-health-report-latest.html"
dated_report="$resolved_output_dir/code-health-report-$today_stamp.html"
largest_report="$resolved_output_dir/largest-files-report.md"
checklist_report="$resolved_output_dir/code-health-checklist.md"
jacoco_baseline="$repo_root/bl-center/jacoco-baseline.properties"
project_health_rules="$repo_root/docs/rules/PROJECT_HEALTH_RULES.md"
report_time_utc="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
report_time_local="$(date +"%Y-%m-%d %H:%M:%S %z")"

mkdir -p "$resolved_output_dir"
tmp_dir="$(mktemp -d)"
cleanup() {
  rm -rf "$tmp_dir"
}
trap cleanup EXIT INT TERM

html_escape() {
  printf '%s' "$1" | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g'
}

invoke_and_measure() {
  name="$1"
  shift
  log_file="$tmp_dir/$name.log"
  start="$(date +%s)"
  if "$@" >"$log_file" 2>&1; then
    exit_code=0
  else
    exit_code=$?
  fi
  end="$(date +%s)"
  duration="$((end - start))"
  cat "$log_file"
  summary="$(tail -n 20 "$log_file" | sed '/^[[:space:]]*$/d')"
  printf '\n__CODE_HEALTH_RESULT__\t%s\t%s\t%s\t%s\n' "$name" "$exit_code" "$duration" "$(printf '%s' "$summary" | tr '\n' '\001')"
}

run_result() {
  name="$1"
  shift
  result="$(invoke_and_measure "$name" "$@")"
  meta_line="$(printf '%s\n' "$result" | awk -F '\t' '/^__CODE_HEALTH_RESULT__/ { print $0 }' | tail -n 1)"
  body="$(printf '%s\n' "$result" | sed '/^__CODE_HEALTH_RESULT__\t/d')"
  if [ -n "$body" ]; then
    printf '%s\n' "$body"
  fi
  exit_code="$(printf '%s' "$meta_line" | awk -F '\t' '{ print $3 + 0 }')"
  duration="$(printf '%s' "$meta_line" | awk -F '\t' '{ print $4 + 0 }')"
  summary="$(printf '%s' "$meta_line" | awk -F '\t' '{ print $5 }' | tr '\001' '\n')"
  if [ "$exit_code" -eq 0 ]; then
    status="PASS"
  else
    status="FAIL"
  fi
  printf '%s\t%s\t%s\t%s\n' "$status" "$exit_code" "$duration" "$summary"
}

extract_overview_rows() {
  awk '
    /^## Overview$/ { seen=1; next }
    /^## / && $0 !~ /^## Overview$/ { seen=0 }
    seen && /^\| [^|]+ \| [^|]+ \| .* \|$/ && $0 !~ /^\| ---/ && $0 !~ /^\| Area / {
      print
    }
  ' "$checklist_report"
}

extract_hotspots() {
  if [ ! -f "$largest_report" ]; then
    return 0
  fi
  awk '
    /^## Top 20 Java Files$/ { seen=1; next }
    /^## / && $0 !~ /^## Top 20 Java Files$/ { seen=0 }
    seen && /^\| [0-9]+ \| `.*` \|$/ {
      if (count < 10) {
        print
        count++
      }
    }
  ' "$largest_report"
}

get_property() {
  file="$1"
  key="$2"
  [ -f "$file" ] || return 0
  awk -F '=' -v wanted="$key" '
    /^[[:space:]]*#/ || /^[[:space:]]*$/ { next }
    $1 == wanted { print $2; exit }
  ' "$file"
}

cd "$repo_root"

largest_output="$(run_result largest-files ./scripts/ci/generate-largest-files-report.sh "$output_dir/largest-files-report.md")"
largest_status="$(printf '%s' "$largest_output" | awk -F '\t' '{ print $1 }')"

gate_output="$(run_result gate ./mvnw -pl common/common-test -Dtest=RepositoryFileHealthGateTest test)"
gate_status="$(printf '%s' "$gate_output" | awk -F '\t' '{ print $1 }')"
gate_exit="$(printf '%s' "$gate_output" | awk -F '\t' '{ print $2 }')"
gate_duration="$(printf '%s' "$gate_output" | awk -F '\t' '{ print $3 }')"
gate_summary="$(printf '%s' "$gate_output" | awk -F '\t' '{ print $4 }' | tr '\n' ' ')"

fast_output="$(run_result fast-feedback ./mvnw test "-Dsurefire.excludedGroups=slow")"
fast_status="$(printf '%s' "$fast_output" | awk -F '\t' '{ print $1 }')"
fast_exit="$(printf '%s' "$fast_output" | awk -F '\t' '{ print $2 }')"
fast_duration="$(printf '%s' "$fast_output" | awk -F '\t' '{ print $3 }')"
fast_summary="$(printf '%s' "$fast_output" | awk -F '\t' '{ print $4 }' | tr '\n' ' ')"

CODE_HEALTH_GATE_STATUS="$gate_status" \
CODE_HEALTH_GATE_EXIT="$gate_exit" \
CODE_HEALTH_GATE_DURATION="$gate_duration" \
CODE_HEALTH_GATE_SUMMARY="$gate_summary" \
CODE_HEALTH_FAST_STATUS="$fast_status" \
CODE_HEALTH_FAST_EXIT="$fast_exit" \
CODE_HEALTH_FAST_DURATION="$fast_duration" \
CODE_HEALTH_FAST_SUMMARY="$fast_summary" \
checklist_output="$(run_result checklist ./scripts/ci/generate-code-health-checklist.sh "$output_dir/code-health-checklist.md" --skip-execution)"
checklist_status="$(printf '%s' "$checklist_output" | awk -F '\t' '{ print $1 }')"

overview_rows_file="$tmp_dir/overview.tsv"
extract_overview_rows > "$overview_rows_file"

hotspots_file="$tmp_dir/hotspots.tsv"
extract_hotspots > "$hotspots_file"

module_rows_file="$tmp_dir/modules.tsv"
awk '
  /<module>/ {
    line=$0
    sub(/^.*<module>/, "", line)
    sub(/<\/module>.*$/, "", line)
    print line
  }
' "$repo_root/pom.xml" | while IFS= read -r module; do
  [ -n "$module" ] || continue
  main_path="$repo_root/$module/src/main/java"
  test_path="$repo_root/$module/src/test/java"
  if [ -d "$main_path" ]; then
    main_count="$(find "$main_path" -type f -name '*.java' | wc -l | tr -d ' ')"
  else
    main_count=0
  fi
  if [ -d "$test_path" ]; then
    test_count="$(find "$test_path" -type f -name '*.java' | wc -l | tr -d ' ')"
  else
    test_count=0
  fi
  if [ "$main_count" -eq 0 ]; then
    ratio="-"
  else
    ratio="$(awk -v a="$test_count" -v b="$main_count" 'BEGIN { printf "%.2f", a / b }')"
  fi
  printf '%s\t%s\t%s\t%s\n' "$module" "$main_count" "$test_count" "$ratio"
done > "$module_rows_file"

if git rev-parse --show-toplevel >/dev/null 2>&1; then
  git_status_file="$tmp_dir/git-status.txt"
  git status --short > "$git_status_file"
  modified_count="$(awk '!/^\?\?/ { count++ } END { print count + 0 }' "$git_status_file")"
  untracked_count="$(awk '/^\?\?/ { count++ } END { print count + 0 }' "$git_status_file")"
  dirty_count="$(wc -l < "$git_status_file" | tr -d ' ')"
else
  git_status_file="$tmp_dir/git-status.txt"
  : > "$git_status_file"
  modified_count=0
  untracked_count=0
  dirty_count=0
fi

codegraph_file="$tmp_dir/codegraph.txt"
if command -v codegraph >/dev/null 2>&1; then
  if codegraph status > "$codegraph_file" 2>&1; then
    codegraph_available=true
  else
    codegraph_available=false
  fi
else
  codegraph_available=false
  printf '%s\n' "codegraph status was unavailable." > "$codegraph_file"
fi

java_over_300='unknown'
java_over_500='unknown'
hard_limit_count='unknown'
if [ -f "$largest_report" ]; then
  java_over_300="$(awk '
    /^## Java Files Over 300 Lines$/ { seen=1; next }
    seen && /^Count: `/ { gsub(/[^0-9]/, "", $0); print $0; exit }
  ' "$largest_report")"
  java_over_500="$(awk '
    /^## Java Files Over 500 Lines$/ { seen=1; next }
    seen && /^Count: `/ { gsub(/[^0-9]/, "", $0); print $0; exit }
  ' "$largest_report")"
  hard_limit_count="$(awk '
    /^## Files Exceeding Current Hard Limits$/ { seen=1; next }
    /^## / && $0 !~ /^## Files Exceeding Current Hard Limits$/ { seen=0 }
    seen && /^\| [0-9]+ \| [0-9]+ \| `.*` \|$/ { count++ }
    END { print count + 0 }
  ' "$largest_report")"
fi

coverage_line="$(get_property "$jacoco_baseline" 'jacoco.minimum.line.coverage' || true)"
coverage_branch="$(get_property "$jacoco_baseline" 'jacoco.minimum.branch.coverage' || true)"

overall_status="PASS"
if [ "$gate_status" = "FAIL" ] || [ "$fast_status" = "FAIL" ]; then
  overall_status="FAIL"
elif awk -F '|' '{ gsub(/^[[:space:]]+|[[:space:]]+$/, "", $3); if ($3 != "PASS") { found=1 } } END { exit(found ? 0 : 1) }' "$overview_rows_file"; then
  overall_status="WATCH"
fi

status_class() {
  case "$1" in
    PASS) printf 'pass' ;;
    FAIL) printf 'fail' ;;
    WATCH) printf 'watch' ;;
    *) printf 'unknown' ;;
  esac
}

summary_cards_file="$tmp_dir/summary-cards.html"
while IFS= read -r row; do
  area="$(printf '%s' "$row" | awk -F '|' '{ gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); print $2 }')"
  status="$(printf '%s' "$row" | awk -F '|' '{ gsub(/^[[:space:]]+|[[:space:]]+$/, "", $3); print $3 }')"
  conclusion="$(printf '%s' "$row" | awk -F '|' '{ gsub(/^[[:space:]]+|[[:space:]]+$/, "", $4); print $4 }')"
  cat >> "$summary_cards_file" <<EOF
<article class="summary-card">
  <div class="status-badge $(status_class "$status")">$(html_escape "$status")</div>
  <h3>$(html_escape "$area")</h3>
  <p>$(html_escape "$conclusion")</p>
</article>
EOF
done < "$overview_rows_file"

hotspot_rows_file="$tmp_dir/hotspot-rows.html"
if [ -s "$hotspots_file" ]; then
  while IFS= read -r row; do
    lines="$(printf '%s' "$row" | awk -F '|' '{ gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); print $2 }')"
    file="$(printf '%s' "$row" | awk -F '|' '{ gsub(/^[[:space:]]+|[[:space:]]+$/, "", $3); gsub(/`/, "", $3); print $3 }')"
    focus='Structural hotspot'
    case "$file" in
      *Repository*|*Jdbc*|*Support*) focus='Persistence responsibilities are too concentrated' ;;
      *Service*) focus='Application orchestration is too large' ;;
      *Controller*) focus='Interface-layer mapping is too heavy' ;;
    esac
    printf '<tr><td>%s</td><td><code>%s</code></td><td>%s</td></tr>\n' "$(html_escape "$lines")" "$(html_escape "$file")" "$(html_escape "$focus")" >> "$hotspot_rows_file"
  done < "$hotspots_file"
else
  printf '<tr><td colspan="3">No hotspot rows were available from <code>largest-files-report.md</code>.</td></tr>\n' > "$hotspot_rows_file"
fi

module_html_file="$tmp_dir/module-rows.html"
while IFS="$(printf '\t')" read -r module main_count test_count ratio; do
  [ -n "$module" ] || continue
  printf '<tr><td><code>%s</code></td><td>%s</td><td>%s</td><td>%s</td></tr>\n' "$(html_escape "$module")" "$(html_escape "$main_count")" "$(html_escape "$test_count")" "$(html_escape "$ratio")" >> "$module_html_file"
done < "$module_rows_file"

worktree_items_file="$tmp_dir/worktree-items.html"
if [ "$dirty_count" -gt 0 ]; then
  head -n 8 "$git_status_file" | while IFS= read -r line; do
    printf '<li><code>%s</code></li>\n' "$(html_escape "$line")" >> "$worktree_items_file"
  done
else
  printf '<li>Working tree is clean.</li>\n' > "$worktree_items_file"
fi

recommendations_file="$tmp_dir/recommendations.html"
if [ "$gate_status" = "FAIL" ]; then
  printf '<li>先修复 <code>RepositoryFileHealthGateTest</code> 失败项，再把文件健康结论视为可信。</li>\n' >> "$recommendations_file"
fi
if [ "$fast_status" = "FAIL" ]; then
  printf '<li>先恢复 <code>mvnw test -Dsurefire.excludedGroups=slow</code> 通过，再把这份报告纳入日常快反馈。</li>\n' >> "$recommendations_file"
fi
if [ "$hard_limit_count" != "unknown" ] && [ "$hard_limit_count" -gt 0 ]; then
  printf '<li>继续收敛临时豁免，把超限的生成型大文件迁出或改成可生成产物。</li>\n' >> "$recommendations_file"
fi
if grep -q 'Repository\|Jdbc\|Support' "$hotspots_file" 2>/dev/null; then
  printf '<li>优先拆分 Repository/Jdbc/Support 热点，把读写、schema 探测和映射职责继续下沉到更小的支持类。</li>\n' >> "$recommendations_file"
fi
if grep -q 'Service' "$hotspots_file" 2>/dev/null; then
  printf '<li>对超大的应用服务按 query/write/import-export 或子域拆分，保持外部 facade 稳定。</li>\n' >> "$recommendations_file"
fi
if grep -q 'Controller' "$hotspots_file" 2>/dev/null; then
  printf '<li>把厚 controller/assembler 的 DTO 组装逻辑继续迁移到显式 assembler 或 mapper。</li>\n' >> "$recommendations_file"
fi
awk -F '\t' '$2 > 0 && $4 != "-" && $4 + 0 < 0.15 { printf "<li>补强 <code>%s</code> 的测试面；当前 test/main 比值约为 <code>%s</code>。</li>\n", $1, $4 }' "$module_rows_file" | head -n 2 >> "$recommendations_file" || true
if [ ! -s "$recommendations_file" ]; then
  printf '<li>保持 <code>bl-center/jacoco-baseline.properties</code> 的 baseline 治理流程，只在有明确验证时更新阈值。</li>\n' > "$recommendations_file"
fi

if [ -f "$project_health_rules" ]; then
  rules_summary='Project health rules are sourced from docs/rules/PROJECT_HEALTH_RULES.md.'
else
  rules_summary='Project health rules file is missing; recommendations are based on current generated artifacts only.'
fi

if [ "$codegraph_available" = "true" ]; then
  codegraph_files="$(grep 'Files:' "$codegraph_file" | awk '{ print $2 }' | head -n 1)"
  codegraph_nodes="$(grep 'Nodes:' "$codegraph_file" | awk '{ print $2 }' | head -n 1)"
  codegraph_edges="$(grep 'Edges:' "$codegraph_file" | awk '{ print $2 }' | head -n 1)"
  codegraph_backend="$(grep 'Backend:' "$codegraph_file" | awk '{ print $2 }' | head -n 1)"
  codegraph_modified="$(grep 'Modified:' "$codegraph_file" | awk '{ print $2 }' | head -n 1)"
  codegraph_block=$(cat <<EOF
<ul class="facts">
  <li>Indexed files: <strong>$(html_escape "${codegraph_files:-unknown}")</strong></li>
  <li>Nodes: <strong>$(html_escape "${codegraph_nodes:-unknown}")</strong></li>
  <li>Edges: <strong>$(html_escape "${codegraph_edges:-unknown}")</strong></li>
  <li>Backend: <strong>$(html_escape "${codegraph_backend:-unknown}")</strong></li>
  <li>Pending modified files: <strong>$(html_escape "${codegraph_modified:-0}")</strong></li>
</ul>
EOF
)
else
  codegraph_block="<p>$(html_escape "$(cat "$codegraph_file")")</p>"
fi

cat > "$latest_report" <<EOF
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Code Health Report</title>
  <style>
    :root {
      --bg: #f4f1ea;
      --panel: #fffdf8;
      --ink: #1f2a30;
      --muted: #67757f;
      --line: #d8d2c4;
      --pass: #2f7d4a;
      --watch: #b87418;
      --fail: #b53a2d;
      --unknown: #5d6d79;
      --shadow: 0 18px 40px rgba(32, 39, 43, 0.08);
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
      background:
        radial-gradient(circle at top right, rgba(205, 160, 67, 0.18), transparent 28%),
        linear-gradient(180deg, #efe8da 0%, var(--bg) 22%, #efe9dc 100%);
      color: var(--ink);
    }
    .page { max-width: 1240px; margin: 0 auto; padding: 32px 20px 56px; }
    .hero, .section { background: var(--panel); border: 1px solid rgba(143,132,114,0.25); border-radius: 22px; box-shadow: var(--shadow); }
    .hero { padding: 28px; margin-bottom: 22px; }
    .hero-top { display: flex; justify-content: space-between; gap: 20px; align-items: flex-start; flex-wrap: wrap; }
    h1, h2, h3 { margin: 0; }
    h1 { font-size: clamp(30px, 5vw, 48px); letter-spacing: -0.04em; }
    .lede { margin-top: 12px; color: var(--muted); max-width: 760px; line-height: 1.6; }
    .status-pill, .status-badge { border: 1px solid currentColor; font-weight: 700; letter-spacing: 0.06em; border-radius: 999px; }
    .status-pill { padding: 10px 16px; min-width: 120px; text-align: center; }
    .hero-grid, .summary-grid, .meta-grid { display: grid; gap: 16px; }
    .hero-grid { margin-top: 24px; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); }
    .metric, .summary-card, .panel { background: rgba(248,245,239,0.88); border: 1px solid rgba(143,132,114,0.22); border-radius: 18px; padding: 16px 18px; }
    .metric span { display: block; color: var(--muted); font-size: 13px; margin-bottom: 8px; }
    .metric strong { font-size: 28px; letter-spacing: -0.03em; }
    .section { padding: 24px; margin-top: 18px; }
    .section h2 { font-size: 24px; margin-bottom: 8px; letter-spacing: -0.03em; }
    .section-intro, .footer-note, .summary-card p, .facts, .list { color: var(--muted); line-height: 1.6; }
    .summary-grid { grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); }
    .summary-card h3 { margin-top: 12px; font-size: 18px; }
    .status-badge { display: inline-flex; align-items: center; justify-content: center; min-width: 78px; padding: 6px 10px; font-size: 12px; }
    .pass { color: var(--pass); background: rgba(47,125,74,0.08); }
    .watch { color: var(--watch); background: rgba(184,116,24,0.1); }
    .fail { color: var(--fail); background: rgba(181,58,45,0.1); }
    .unknown { color: var(--unknown); background: rgba(93,109,121,0.1); }
    .table-wrap { overflow-x: auto; border: 1px solid rgba(143,132,114,0.2); border-radius: 16px; }
    table { width: 100%; border-collapse: collapse; min-width: 720px; background: #fffefb; }
    th, td { padding: 14px 16px; border-bottom: 1px solid rgba(143,132,114,0.16); text-align: left; vertical-align: top; font-size: 14px; line-height: 1.55; }
    th { background: rgba(244,238,228,0.94); color: #334047; font-size: 12px; text-transform: uppercase; letter-spacing: 0.05em; }
    code { font-family: "Cascadia Code", "Consolas", monospace; font-size: 0.92em; color: #183447; }
    pre { margin: 10px 0 0; padding: 12px; border-radius: 12px; background: #f4f1ea; border: 1px solid rgba(143,132,114,0.2); white-space: pre-wrap; word-break: break-word; font-size: 12px; line-height: 1.5; }
    details summary { cursor: pointer; color: #2d4958; font-weight: 600; }
    .meta-grid { grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); }
    .facts, .list { margin: 0; padding-left: 18px; }
    @media (max-width: 720px) {
      .page { padding: 18px 14px 40px; }
      .hero, .section { padding: 18px; border-radius: 18px; }
      table { min-width: 620px; }
    }
  </style>
</head>
<body>
  <div class="page">
    <section class="hero">
      <div class="hero-top">
        <div>
          <h1>Repository Code Health Report</h1>
          <p class="lede">这份报告基于仓库现有的 <code>largest-files-report</code>、<code>code-health-checklist</code>、JaCoCo baseline 与项目健康规则生成，采用“静态分析 + 实际验证”模式，并保留 dirty worktree 上下文。</p>
        </div>
        <div class="status-pill $(status_class "$overall_status")">$(html_escape "$overall_status")</div>
      </div>
      <div class="hero-grid">
        <div class="metric"><span>Generated At</span><strong>$(html_escape "$report_time_local")</strong></div>
        <div class="metric"><span>Hotspots &gt; 300 Lines</span><strong>$(html_escape "${java_over_300:-unknown}")</strong></div>
        <div class="metric"><span>Gate / Fast Feedback</span><strong>$(html_escape "$gate_status / $fast_status")</strong></div>
        <div class="metric"><span>Dirty Worktree</span><strong>$( [ "$dirty_count" -gt 0 ] && printf 'Yes' || printf 'No' )</strong></div>
      </div>
    </section>

    <section class="section">
      <h2>执行摘要</h2>
      <p class="section-intro">摘要卡片直接来自刷新后的 <code>docs/reports/code-health-checklist.md</code>。统一入口负责执行验证并把结果注入到 Markdown 产物，再渲染成 HTML。</p>
      <div class="summary-grid">
        $(cat "$summary_cards_file")
      </div>
    </section>

    <section class="section">
      <h2>验证结果</h2>
      <p class="section-intro">这里展示本次运行真正执行过的校验命令、耗时、退出码和失败摘要。即使命令失败，HTML 仍会生成。</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Command</th><th>Status</th><th>Exit</th><th>Duration</th><th>Summary</th></tr>
          </thead>
          <tbody>
            <tr>
              <td><code>./mvnw -pl common/common-test -Dtest=RepositoryFileHealthGateTest test</code></td>
              <td><span class="status-badge $(status_class "$gate_status")">$(html_escape "$gate_status")</span></td>
              <td>$(html_escape "$gate_exit")</td>
              <td>$(html_escape "$gate_duration")s</td>
              <td><details><summary>Summary</summary><pre>$(html_escape "$gate_summary")</pre></details></td>
            </tr>
            <tr>
              <td><code>./mvnw test -Dsurefire.excludedGroups=slow</code></td>
              <td><span class="status-badge $(status_class "$fast_status")">$(html_escape "$fast_status")</span></td>
              <td>$(html_escape "$fast_exit")</td>
              <td>$(html_escape "$fast_duration")s</td>
              <td><details><summary>Summary</summary><pre>$(html_escape "$fast_summary")</pre></details></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="footer-note">Coverage baseline 取自 <code>bl-center/jacoco-baseline.properties</code>：line = <strong>$(html_escape "${coverage_line:-unknown}")</strong>，branch = <strong>$(html_escape "${coverage_branch:-unknown}")</strong>。这是治理基线，不是本次实时覆盖率。</p>
    </section>

    <section class="section">
      <h2>结构热点</h2>
      <p class="section-intro">热点来自刷新后的 <code>docs/reports/largest-files-report.md</code>。当前 hard-limit 超限文件数为 <strong>$(html_escape "${hard_limit_count:-unknown}")</strong>，Java 500+ 文件数为 <strong>$(html_escape "${java_over_500:-unknown}")</strong>。</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Lines</th><th>File</th><th>Why It Matters</th></tr>
          </thead>
          <tbody>
            $(cat "$hotspot_rows_file")
          </tbody>
        </table>
      </div>
    </section>

    <section class="section">
      <h2>模块测试分布</h2>
      <p class="section-intro">统计按根 POM 中声明的 Maven module 计算，帮助快速识别测试面明显偏薄的模块。</p>
      <div class="table-wrap">
        <table>
          <thead>
            <tr><th>Module</th><th>Main Java</th><th>Test Java</th><th>Test/Main Ratio</th></tr>
          </thead>
          <tbody>
            $(cat "$module_html_file")
          </tbody>
        </table>
      </div>
    </section>

    <section class="section">
      <h2>工作区上下文</h2>
      <p class="section-intro">报告不会因为工作区未清理而拒绝生成，但会把当前上下文显式带出来，避免把治理结论误当成干净基线。</p>
      <div class="meta-grid">
        <article class="panel">
          <h3>Git Worktree</h3>
          <ul class="facts">
            <li>Dirty: <strong>$( [ "$dirty_count" -gt 0 ] && printf 'Yes' || printf 'No' )</strong></li>
            <li>Tracked changes: <strong>$(html_escape "$modified_count")</strong></li>
            <li>Untracked files: <strong>$(html_escape "$untracked_count")</strong></li>
          </ul>
          <ul class="list">
            $(cat "$worktree_items_file")
          </ul>
        </article>
        <article class="panel">
          <h3>CodeGraph</h3>
          $codegraph_block
        </article>
        <article class="panel">
          <h3>治理来源</h3>
          <ul class="facts">
            <li>Largest files report: <strong>$(html_escape "$largest_status")</strong></li>
            <li>Checklist report: <strong>$(html_escape "$checklist_status")</strong></li>
            <li>Generated UTC: <strong>$(html_escape "$report_time_utc")</strong></li>
          </ul>
          <p class="footer-note">$(html_escape "$rules_summary")</p>
        </article>
      </div>
    </section>

    <section class="section">
      <h2>建议动作</h2>
      <p class="section-intro">这些动作按当前验证结果、热点类型和模块测试分布自动整理，优先对齐仓库现有的项目健康规则。</p>
      <ul class="list">
        $(cat "$recommendations_file")
      </ul>
    </section>
  </div>
</body>
</html>
EOF

cp "$latest_report" "$dated_report"

if [ "$largest_status" = "FAIL" ] || [ "$checklist_status" = "FAIL" ] || [ "$gate_status" = "FAIL" ] || [ "$fast_status" = "FAIL" ]; then
  exit 1
fi
