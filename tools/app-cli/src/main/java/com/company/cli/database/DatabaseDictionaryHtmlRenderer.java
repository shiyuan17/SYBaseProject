package com.company.cli.database;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
class DatabaseDictionaryHtmlRenderer {

    String render(DatabaseDictionaryReport report) {
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <title>数据库字典</title>
              <style>
                :root {
                  --bg: #f4f1ea;
                  --panel: #fffdf8;
                  --ink: #1f2a30;
                  --muted: #67757f;
                  --line: #d8d2c4;
                  --ok: #2f7d4a;
                  --warning: #b87418;
                  --shadow: 0 18px 40px rgba(32, 39, 43, 0.08);
                }
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  font-family: "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif;
                  color: var(--ink);
                  background:
                    radial-gradient(circle at top right, rgba(205, 160, 67, 0.18), transparent 28%),
                    linear-gradient(180deg, #efe8da 0%, var(--bg) 20%, #efe9dc 100%);
                }
                .page { max-width: 1280px; margin: 0 auto; padding: 32px 20px 56px; }
                .hero, .section, .table-card, .summary-card, .owner-card {
                  background: var(--panel);
                  border: 1px solid rgba(143, 132, 114, 0.25);
                  border-radius: 22px;
                  box-shadow: var(--shadow);
                }
                .hero, .section { padding: 24px; margin-bottom: 18px; }
                h1, h2, h3, h4 { margin: 0; }
                h1 { font-size: clamp(30px, 5vw, 46px); letter-spacing: -0.04em; }
                h2 { font-size: 24px; margin-bottom: 12px; letter-spacing: -0.03em; }
                h3 { font-size: 20px; }
                .lede, .muted, .directory-list, td, li, code, .empty-state { color: var(--muted); }
                .hero-top, .toolbar { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
                .status-pill {
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                  min-width: 128px;
                  padding: 10px 16px;
                  border-radius: 999px;
                  font-weight: 700;
                  letter-spacing: 0.06em;
                  border: 1px solid currentColor;
                }
                .status-pill.ok { color: var(--ok); background: rgba(47, 125, 74, 0.1); }
                .status-pill.warning { color: var(--warning); background: rgba(184, 116, 24, 0.12); }
                .summary-grid { display: grid; gap: 16px; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); }
                .summary-card, .owner-card { padding: 16px 18px; }
                .summary-card span { display: block; font-size: 13px; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.06em; }
                .summary-card strong { font-size: 28px; letter-spacing: -0.03em; color: var(--ink); }
                label { font-weight: 600; color: var(--ink); }
                input[type="search"] {
                  min-width: min(360px, 100%);
                  padding: 12px 14px;
                  border-radius: 14px;
                  border: 1px solid rgba(143, 132, 114, 0.28);
                  background: rgba(255, 255, 255, 0.92);
                  font: inherit;
                }
                .directory-list { margin: 0; padding-left: 18px; line-height: 1.7; }
                .source-block { margin-top: 18px; }
                .source-header { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 12px; }
                .owner-card { margin-bottom: 14px; }
                details.owner-card > summary, details.table-card > summary { cursor: pointer; list-style: none; }
                details.owner-card > summary::-webkit-details-marker, details.table-card > summary::-webkit-details-marker { display: none; }
                .table-card { padding: 16px 18px; margin-top: 12px; }
                .table-summary { display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; align-items: baseline; }
                .table-meta { display: flex; gap: 10px; flex-wrap: wrap; margin-top: 10px; }
                .badge {
                  display: inline-flex;
                  align-items: center;
                  padding: 5px 10px;
                  border-radius: 999px;
                  border: 1px solid rgba(143, 132, 114, 0.22);
                  background: rgba(244, 238, 228, 0.94);
                  font-size: 12px;
                  color: #334047;
                }
                .table-anchor { color: #183447; text-decoration: none; }
                .table-anchor:hover { text-decoration: underline; }
                .table-wrap { overflow-x: auto; border: 1px solid rgba(143, 132, 114, 0.18); border-radius: 16px; margin-top: 14px; }
                table { width: 100%; border-collapse: collapse; min-width: 760px; background: #fffefb; }
                th, td {
                  padding: 12px 14px;
                  border-bottom: 1px solid rgba(143, 132, 114, 0.16);
                  text-align: left;
                  vertical-align: top;
                  font-size: 14px;
                  line-height: 1.55;
                }
                th {
                  background: rgba(244, 238, 228, 0.94);
                  color: #334047;
                  font-size: 12px;
                  text-transform: uppercase;
                  letter-spacing: 0.05em;
                }
                code {
                  font-family: "Cascadia Code", "Consolas", monospace;
                  background: rgba(23, 74, 124, 0.06);
                  border-radius: 6px;
                  padding: 1px 5px;
                }
                .empty-state {
                  padding: 18px;
                  border-radius: 16px;
                  border: 1px dashed rgba(143, 132, 114, 0.38);
                  background: rgba(255, 255, 255, 0.6);
                }
                @media (max-width: 900px) {
                  .page { padding: 20px 14px 40px; }
                  .hero, .section { padding: 18px; }
                  input[type="search"] { min-width: 100%; }
                }
              </style>
            </head>
            <body>
              <div class="page">
            """);
        appendHero(html, report);
        appendDirectory(html, report);
        appendSources(html, report);
        appendFooter(html);
        return html.toString();
    }

    private void appendHero(StringBuilder html, DatabaseDictionaryReport report) {
        html.append("<section class=\"hero\"><div class=\"hero-top\"><div>");
        html.append("<h1>数据库字典</h1>");
        html.append("<p class=\"lede\">基于当前可见达梦对象实时采集的结构说明，覆盖表、字段、约束、索引与外键信息。</p>");
        html.append("</div><div class=\"status-pill ").append(report.status().cssClass()).append("\">")
            .append(escapeHtml(report.status().label())).append("</div></div>");
        html.append("<div class=\"summary-grid\" style=\"margin-top:24px;\">");
        appendSummaryCard(html, "生成时间", escapeHtml(report.generatedAt().toString()));
        appendSummaryCard(html, "采集范围", escapeHtml(report.scope().toString()));
        appendSummaryCard(html, "数据源", Integer.toString(report.summary().sourceCount()));
        appendSummaryCard(html, "Owner", Integer.toString(report.summary().ownerCount()));
        appendSummaryCard(html, "表", Integer.toString(report.summary().tableCount()));
        appendSummaryCard(html, "字段", Integer.toString(report.summary().columnCount()));
        appendSummaryCard(html, "索引", Integer.toString(report.summary().indexCount()));
        appendSummaryCard(html, "外键", Integer.toString(report.summary().foreignKeyCount()));
        html.append("</div></section>");
    }

    private void appendDirectory(StringBuilder html, DatabaseDictionaryReport report) {
        html.append("<section class=\"section\"><div class=\"toolbar\"><div><h2>目录与筛选</h2>");
        html.append("<p class=\"lede\">按 owner 或表名即时筛选，点击目录跳转到对应表卡片。</p></div>");
        html.append("<div><label for=\"table-search\">搜索</label><br><input id=\"table-search\" type=\"search\" placeholder=\"输入 owner / table 名称\"></div></div>");
        if (report.summary().tableCount() == 0) {
            html.append("<div class=\"empty-state\">未采集到任何表，请检查当前账号权限、连接参数或目标库状态。</div>");
        } else {
            html.append("<ul class=\"directory-list\">");
            for (DatabaseSourceReport source : report.sources()) {
                for (DatabaseOwnerReport owner : source.owners()) {
                    for (DatabaseTableReport table : owner.tables()) {
                        String searchText = (owner.owner() + " " + table.tableName()).toLowerCase(Locale.ROOT);
                        html.append("<li data-directory-item data-search=\"").append(escapeHtml(searchText)).append("\">");
                        html.append("<a class=\"table-anchor\" href=\"#").append(tableAnchorId(table.owner(), table.tableName())).append("\">")
                            .append(escapeHtml(owner.owner())).append(".").append(escapeHtml(table.tableName())).append("</a>");
                        if (table.comment() != null) {
                            html.append(" - ").append(escapeHtml(table.comment()));
                        }
                        html.append("</li>");
                    }
                }
            }
            html.append("</ul>");
        }
        html.append("</section>");
    }

    private void appendSources(StringBuilder html, DatabaseDictionaryReport report) {
        html.append("<section class=\"section\"><h2>数据源明细</h2>");
        if (report.sources().isEmpty()) {
            html.append("<div class=\"empty-state\">当前没有可展示的数据源。</div>");
        }
        for (DatabaseSourceReport source : report.sources()) {
            html.append("<div class=\"source-block\"><div class=\"source-header\"><div>");
            html.append("<h3>").append(escapeHtml(String.join(" + ", source.labels()))).append("</h3>");
            html.append("<p class=\"muted\"><code>").append(escapeHtml(source.url())).append("</code> / <code>")
                .append(escapeHtml(source.username())).append("</code></p></div>");
            html.append("<div class=\"badge\">owner 数: ").append(source.owners().size()).append("</div></div>");
            if (source.owners().isEmpty()) {
                html.append("<div class=\"empty-state\">该连接未返回任何表对象。</div>");
            }
            for (DatabaseOwnerReport owner : source.owners()) {
                html.append("<details class=\"owner-card\" open><summary><h4>")
                    .append(escapeHtml(owner.owner())).append(" <span class=\"muted\">(")
                    .append(owner.tables().size()).append(" tables)</span></h4></summary>");
                for (DatabaseTableReport table : owner.tables()) {
                    appendTableCard(html, owner.owner(), table);
                }
                html.append("</details>");
            }
            html.append("</div>");
        }
        html.append("</section>");
    }

    private void appendTableCard(StringBuilder html, String owner, DatabaseTableReport table) {
        String anchorId = tableAnchorId(owner, table.tableName());
        String searchText = (owner + " " + table.tableName() + " " + Objects.toString(table.comment(), ""))
            .toLowerCase(Locale.ROOT);
        html.append("<details class=\"table-card\" id=\"").append(anchorId).append("\" data-search=\"")
            .append(escapeHtml(searchText)).append("\"><summary>");
        html.append("<div class=\"table-summary\"><div><h4>")
            .append(escapeHtml(table.tableName())).append("</h4>");
        if (table.comment() != null) {
            html.append("<p class=\"muted\">").append(escapeHtml(table.comment())).append("</p>");
        }
        html.append("</div><a class=\"table-anchor\" href=\"#").append(anchorId).append("\"># 直达</a></div>");
        html.append("<div class=\"table-meta\">");
        html.append("<span class=\"badge\">字段 ").append(table.columns().size()).append("</span>");
        html.append("<span class=\"badge\">键约束 ").append(table.uniqueConstraints().size()).append("</span>");
        html.append("<span class=\"badge\">外键 ").append(table.foreignKeys().size()).append("</span>");
        html.append("<span class=\"badge\">索引 ").append(table.indexes().size()).append("</span>");
        html.append("</div></summary>");
        appendColumnTable(html, table.columns());
        appendConstraintTable(html, table.uniqueConstraints(), table.foreignKeys());
        appendIndexTable(html, table.indexes());
        html.append("</details>");
    }

    private void appendColumnTable(StringBuilder html, List<DatabaseColumnReport> columns) {
        html.append("<div class=\"table-wrap\"><table><thead><tr>")
            .append("<th>列名</th><th>类型</th><th>非空</th><th>默认值</th><th>主键</th><th>键约束</th><th>外键</th><th>说明</th>")
            .append("</tr></thead><tbody>");
        for (DatabaseColumnReport column : columns) {
            html.append("<tr><td><code>").append(escapeHtml(column.name())).append("</code></td>")
                .append("<td>").append(escapeHtml(column.type())).append("</td>")
                .append("<td>").append(column.nullable() ? "否" : "是").append("</td>")
                .append("<td>").append(escapeHtml(orDash(column.defaultValue()))).append("</td>")
                .append("<td>").append(column.primaryKey() ? "是" : "-").append("</td>")
                .append("<td>").append(escapeHtml(joinNames(column.keyConstraintNames()))).append("</td>")
                .append("<td>").append(escapeHtml(joinNames(column.foreignKeyNames()))).append("</td>")
                .append("<td>").append(escapeHtml(orDash(column.comment()))).append("</td></tr>");
        }
        html.append("</tbody></table></div>");
    }

    private void appendConstraintTable(
        StringBuilder html,
        List<DatabaseUniqueConstraintReport> uniqueConstraints,
        List<DatabaseForeignKeyReport> foreignKeys
    ) {
        html.append("<div class=\"table-wrap\"><table><thead><tr>")
            .append("<th>约束类型</th><th>名称</th><th>列</th><th>引用目标</th></tr></thead><tbody>");
        if (uniqueConstraints.isEmpty() && foreignKeys.isEmpty()) {
            html.append("<tr><td colspan=\"4\">-</td></tr>");
        }
        for (DatabaseUniqueConstraintReport constraint : uniqueConstraints) {
            html.append("<tr><td>").append(constraint.primaryKey() ? "PRIMARY KEY" : "UNIQUE").append("</td>")
                .append("<td><code>").append(escapeHtml(constraint.name())).append("</code></td>")
                .append("<td>").append(escapeHtml(joinNames(constraint.columns()))).append("</td><td>-</td></tr>");
        }
        for (DatabaseForeignKeyReport foreignKey : foreignKeys) {
            html.append("<tr><td>FOREIGN KEY</td>")
                .append("<td><code>").append(escapeHtml(foreignKey.name())).append("</code></td>")
                .append("<td>").append(escapeHtml(joinNames(foreignKey.columns()))).append("</td>")
                .append("<td>").append(escapeHtml(foreignKey.referencedOwner())).append(".")
                .append(escapeHtml(foreignKey.referencedTable()))
                .append(" (").append(escapeHtml(joinNames(foreignKey.referencedColumns()))).append(")")
                .append("</td></tr>");
        }
        html.append("</tbody></table></div>");
    }

    private void appendIndexTable(StringBuilder html, List<DatabaseIndexReport> indexes) {
        html.append("<div class=\"table-wrap\"><table><thead><tr>")
            .append("<th>索引名</th><th>唯一性</th><th>类型</th><th>列</th></tr></thead><tbody>");
        if (indexes.isEmpty()) {
            html.append("<tr><td colspan=\"4\">-</td></tr>");
        }
        for (DatabaseIndexReport index : indexes) {
            html.append("<tr><td><code>").append(escapeHtml(index.name())).append("</code></td>")
                .append("<td>").append(escapeHtml(index.uniqueness())).append("</td>")
                .append("<td>").append(escapeHtml(index.indexType())).append("</td>")
                .append("<td>").append(escapeHtml(joinNames(index.columns()))).append("</td></tr>");
        }
        html.append("</tbody></table></div>");
    }

    private void appendSummaryCard(StringBuilder html, String label, String value) {
        html.append("<article class=\"summary-card\"><span>")
            .append(label)
            .append("</span><strong>")
            .append(value)
            .append("</strong></article>");
    }

    private void appendFooter(StringBuilder html) {
        html.append("""
              <script>
                const searchInput = document.getElementById('table-search');
                const filterable = document.querySelectorAll('[data-search]');
                if (searchInput) {
                  searchInput.addEventListener('input', function () {
                    const keyword = searchInput.value.trim().toLowerCase();
                    filterable.forEach(function (node) {
                      const haystack = (node.getAttribute('data-search') || '').toLowerCase();
                      node.style.display = keyword === '' || haystack.includes(keyword) ? '' : 'none';
                    });
                  });
                }
              </script>
            </body>
            </html>
            """);
    }

    private String tableAnchorId(String owner, String tableName) {
        return "table-" + sanitizeId(owner) + "-" + sanitizeId(tableName);
    }

    private String sanitizeId(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String joinNames(List<String> names) {
        return names == null || names.isEmpty() ? "-" : String.join(", ", names);
    }

    private String orDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
