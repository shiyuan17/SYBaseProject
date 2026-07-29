# SY Base Project

`SYBaseProject` 鏄竴涓熀浜?`Java 17 + Spring Boot 3 + Maven Wrapper` 鐨勫妯″潡鐥呯悊涓氬姟宸ョ▼銆傚綋鍓嶄粨搴撳凡缁忎笉鍐嶅彧鏄?DDD 鑴氭墜鏋剁ず渚嬶紝`bl-center`銆乣auth-center`銆佸叕鍏卞熀纭€妯″潡銆丗lyway 杩佺Щ鍜屽鏉?M1-M4 涓氬姟閾捐矾閮藉凡钀藉湴銆?
## 褰撳墠妯″潡

- `bl-center`锛氱梾鐞嗕笟鍔′富妯″潡锛屽凡瑕嗙洊 M1-M4 澶氶樁娈佃兘鍔?- `auth-center`锛氳璇侀壌鏉冧笌鐧诲綍鏀寔妯″潡
- `common/common-core`锛氶€氱敤閿欒鐮併€佸紓甯稿拰鍊煎璞＄害瀹?- `common/common-security`锛氶壌鏉冦€佸畨鍏ㄤ笌瀵嗙爜鑳藉姏
- `common/common-web`锛氱粺涓€鍝嶅簲銆佸紓甯稿鐞嗐€乄eb 鏀拺
- `common/common-test`锛氭祴璇曞熀绫汇€佷粨搴撴不鐞嗘牎楠屼笌闆嗘垚娴嬭瘯鏀拺
- `user-center`锛氫繚鐣欑殑鍒嗗眰缁撴瀯绀轰緥妯″潡
- `tools/app-cli`锛氬懡浠よ宸ュ叿绀轰緥妯″潡

## 褰撳墠鐘舵€?
- M1锛氱郴缁熺鐞嗐€佽彍鍗曟潈闄愩€佺郴缁熼厤缃€佺紪鍙疯鍒欏凡鍙繍琛?- M2锛氱敵璇峰崟銆佹爣鏈櫥璁般€佸浐瀹氥€佹帴鏀躲€佽繍閫佷富娴佺▼宸茶惤鍦?- M3锛氭妧鏈祦绋嬩富閾捐矾宸茶惤鍦帮紝鏀寔寰呭姙鏌ヨ銆佹妧鏈拷韪€佽繑宸ヤ笌 QC 鍘嗗彶鍙鍖?- M4锛氳瘖鏂姤鍛娿€佷慨璁€佸尰鍢便€佷細璇婄瓑璇婃柇闂幆鑳藉姏宸叉帴鍏?
璇︾粏鐩綍璇存槑瑙?[docs/guides/PROJECT_DIRECTORY.md](./docs/guides/PROJECT_DIRECTORY.md)锛屾枃妗ｅ鑸 [docs/README.md](./docs/README.md)銆?
## 鍚姩鏂瑰紡

鍓嶇疆鏉′欢锛?
- `JDK 17`
- `JAVA_HOME` 鎸囧悜 JDK 17锛屾垨鏈満 `java` 榛樿灏辨槸 JDK 17

寤鸿浼樺厛浣跨敤 Maven Wrapper锛岄伩鍏嶆湰鏈?Maven/JDK 鐗堟湰婕傜Щ銆?
鏍￠獙鏋勫缓鐜锛?
```bash
./mvnw -version
```

棣栨鍏嬮殕鍚庡畨瑁呬緷璧栧埌浠撳簱鍐呮湰鍦扮紦瀛橈細

```bash
./mvnw -B -ntp -Dmaven.repo.local=.m2/repository install -DskipTests
```

杩愯鍏ㄩ儴娴嬭瘯锛?
```bash
./mvnw test
```

## 本地开发启动

- `bl-center` 与 `auth-center` 已包含 `spring-boot-devtools`，支持开发态自动重启。
- 命令行启动脚本现在按操作系统拆分到 `scripts/dev/windows/` 与 `scripts/dev/unix/`。
- 启动脚本会先执行一次 `-pl <module> -am compile`，把 `common/*/target/classes` 加入运行时 classpath，并在后台持续监听源码变化触发 DevTools 重启。

推荐入口：

```bash
./scripts/dev/unix/run-bl-center-dev.sh
./scripts/dev/unix/run-auth-center-dev.sh
```

Windows 可用：

```powershell
.\run-bl-center-dev.cmd
.\scripts\dev\windows\run-bl-center-dev.cmd
```

这些命令只能在 `D:\Github\JW\SYBaseProject` 仓库根目录运行，不适用于 `D:\Github\JW\SYBaseProjectWeb`。

如果遇到 `ClassNotFoundException: com.company.bl.BlCenterApplication`，可重新编译：

```powershell
.\mvnw.cmd -pl bl-center -am compile -DskipTests
```

如果遇到 `ClassNotFoundException: com.company.auth.AuthCenterApplication`，可重新编译：

```powershell
.\mvnw.cmd -pl auth-center -am compile -DskipTests
```

## 目录与治理说明
- `scripts/dev/`：本地开发脚本，实际实现按 `windows/` 与 `unix/` 分层
- `scripts/prod/`：生产启动、同步发布与 `config/` 配置模板
- `scripts/migration/`：Flyway 与迁移辅助脚本
- `docs/`：协作规范、工程说明、计划与治理文档
- `deploy/`：本地 GitLab 与部署样例
- `gateway/`、`order-center/`、`ai-center/`、`admin-web/`：当前仍为预留扩展位
- 顶层 `infrastructure/`：平台级沉淀预留目录，尚未作为独立可运行模块交付

## 绀轰緥鎺ュ彛

- `POST /api/v1/applications`
- `POST /api/v1/specimens/register`
- `GET /api/v1/technical-tasks/pending`
- `GET /api/v1/pathology-cases/{id}/technical-tracking`
- `GET /api/v1/pathology-cases/{id}/diagnostic-workbench`

涓氬姟鎺ュ彛榛樿浼氳嚜鍔ㄥ寘瑁呬负缁熶竴杩斿洖浣擄紱濡傞渶杩斿洖鍘熷鍐呭锛屽彲浣跨敤 `@IgnoreApiResponseWrap` 璺宠繃鍖呰銆?

## Frontend Ownership

- `admin-web/` in this repository remains a placeholder only.
- The active front-end workspace, page flows, SOP assets, and M7 trial materials live in `D:\Github\JW\SYBaseProjectWeb`.
- This repository remains the source of truth for backend APIs, security rules, integration tests, and milestone gates.
