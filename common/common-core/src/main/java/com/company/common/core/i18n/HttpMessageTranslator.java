package com.company.common.core.i18n;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpMessageTranslator {

    private static final Pattern REQUIRED_PATTERN = Pattern.compile("^(.+) is required$");
    private static final Pattern NOT_BLANK_PATTERN = Pattern.compile("^(.+) must not be blank$");
    private static final Pattern NOT_EMPTY_PATTERN = Pattern.compile("^(.+) must not be empty$");
    private static final Pattern NOT_EXCEED_PATTERN = Pattern.compile("^(.+) must not exceed (\\d+) characters$");
    private static final Pattern NOT_NEGATIVE_PATTERN = Pattern.compile("^(.+) must not be negative$");
    private static final Pattern BETWEEN_PATTERN = Pattern.compile("^(.+) must be between (\\d+) and (\\d+)$");
    private static final Pattern UNSUPPORTED_WITH_VALUE_PATTERN = Pattern.compile("^Unsupported (.+): (.+)$");
    private static final Pattern NO_PERMISSION_PATTERN = Pattern.compile("^User does not have permission: (.+)$");
    private static final Pattern CONSTRAINT_WITH_PATH_PATTERN = Pattern.compile("^(.+?):\\s+(.+)$");

    private static final Map<String, String> EXACT_TRANSLATIONS = Map.ofEntries(
        Map.entry("A replacement archive position is required for return", "归还时必须提供新的归档位置"),
        Map.entry("Access token encoding is invalid", "访问令牌编码无效"),
        Map.entry("Access token format is invalid", "访问令牌格式无效"),
        Map.entry("Access token is expired", "访问令牌已过期"),
        Map.entry("Access token is invalid", "访问令牌无效"),
        Map.entry("Access token is revoked", "访问令牌已失效"),
        Map.entry("Access token is revoked or unavailable", "访问令牌已失效或不可用"),
        Map.entry("Access token issuer is invalid", "访问令牌签发者无效"),
        Map.entry("Access token payload is invalid", "访问令牌载荷无效"),
        Map.entry("Access token signature is invalid", "访问令牌签名无效"),
        Map.entry("Access token verification failed", "访问令牌校验失败"),
        Map.entry("Active technical task already exists", "活动中的技术任务已存在"),
        Map.entry("Active technical task not found", "未找到活动中的技术任务"),
        Map.entry("Application field is invalid", "申请单字段无效"),
        Map.entry("Application id is invalid", "申请单ID无效"),
        Map.entry("Application has entered downstream workflow and cannot be rewritten in the registration workbench", "申请单已进入下游流程，无法在登记工作台重新填写"),
        Map.entry("Application not found", "申请单不存在"),
        Map.entry("Application number already exists", "申请单号已存在"),
        Map.entry("Application number is invalid", "申请单号无效"),
        Map.entry("Application number is required", "申请单号不能为空"),
        Map.entry("Application status is invalid", "申请单状态无效"),
        Map.entry("Application workbench record not found", "申请登记工作台记录不存在"),
        Map.entry("Archive cabinet code already exists", "归档柜编码已存在"),
        Map.entry("Archive cabinet is disabled", "归档柜已停用"),
        Map.entry("Archive cabinet not found", "归档柜不存在"),
        Map.entry("Archive position is not available", "归档位置不可用"),
        Map.entry("Archive position not found", "归档位置不存在"),
        Map.entry("Archive record not found", "归档记录不存在"),
        Map.entry("Archived material is already borrowed", "归档材料已借出"),
        Map.entry("Archived material is currently borrowed", "归档材料当前已被借出"),
        Map.entry("Authentication is required", "需要登录认证"),
        Map.entry("Authorization bearer token is required", "缺少 Authorization Bearer 令牌"),
        Map.entry("Billing record not found", "计费记录不存在"),
        Map.entry("Body part code already exists", "部位编码已存在"),
        Map.entry("Body part is referenced by templates", "部位已被模板引用"),
        Map.entry("Body part not found", "部位不存在"),
        Map.entry("Body part still has child nodes", "部位下仍有子节点"),
        Map.entry("Charge item code already exists", "收费项目编码已存在"),
        Map.entry("Charge item not found", "收费项目不存在"),
        Map.entry("Config category code already exists", "配置分类编码已存在"),
        Map.entry("Config category not found", "配置分类不存在"),
        Map.entry("Config category still has children or items", "配置分类下仍有子分类或配置项"),
        Map.entry("Config item not found", "配置项不存在"),
        Map.entry("Config key already exists", "配置键已存在"),
        Map.entry("Consultation already completed", "会诊已完成"),
        Map.entry("Consultation is not active", "会诊未处于进行中状态"),
        Map.entry("Consultation not found", "会诊不存在"),
        Map.entry("Consultation participant not found", "会诊参与人不存在"),
        Map.entry("Current account is disabled", "当前账号已被禁用"),
        Map.entry("Dehydration batch not found", "脱水批次不存在"),
        Map.entry("Department code already exists", "科室编码已存在"),
        Map.entry("Department is referenced by users", "科室已被用户引用"),
        Map.entry("Department not found", "科室不存在"),
        Map.entry("Department still has child nodes", "科室下仍有子节点"),
        Map.entry("Diagnostic task cannot be started", "诊断任务无法启动"),
        Map.entry("Diagnostic task is not assignable", "诊断任务不可分配"),
        Map.entry("Diagnostic task is not editable", "诊断任务不可编辑"),
        Map.entry("Diagnostic task not found", "诊断任务不存在"),
        Map.entry("Direct receipt items must belong to the same application", "直接接收的标本项必须属于同一申请单"),
        Map.entry("Draft report already exists", "报告草稿已存在"),
        Map.entry("Embedding box not found", "包埋盒不存在"),
        Map.entry("Equipment code already exists", "设备编码已存在"),
        Map.entry("Equipment record not found", "设备记录不存在"),
        Map.entry("External integration is unavailable", "外部集成不可用"),
        Map.entry("External order number or application date is required", "外部单号或申请日期不能为空"),
        Map.entry("Failed quality checks must provide issue codes", "质检不合格时必须提供问题编码"),
        Map.entry("Failed to generate business number", "业务编号生成失败"),
        Map.entry("Grossing image file exceeds size limit", "取材图片文件超过大小限制"),
        Map.entry("Grossing image file is required", "必须上传取材图片文件"),
        Map.entry("Grossing image not found", "取材图片不存在"),
        Map.entry("Grossing image upload failed", "取材图片上传失败"),
        Map.entry("Guideline category code already exists", "指南分类编码已存在"),
        Map.entry("Guideline category not found", "指南分类不存在"),
        Map.entry("Guideline category still has children or guidelines", "指南分类下仍有子分类或指南"),
        Map.entry("Guideline code already exists", "指南编码已存在"),
        Map.entry("Guideline not found", "指南不存在"),
        Map.entry("Historical import job not found", "历史导入任务不存在"),
        Map.entry("Imported application already exists", "已导入的申请单已存在"),
        Map.entry("Integration task not found for billing record", "未找到与计费记录关联的集成任务"),
        Map.entry("Internal server error", "服务器内部错误"),
        Map.entry("Invalid grossing image path", "取材图片路径无效"),
        Map.entry("Login name or password is incorrect", "登录名或密码错误"),
        Map.entry("Login result must be SUCCESS or FAILED", "登录结果必须为 SUCCESS 或 FAILED"),
        Map.entry("Login result must not be blank", "登录结果不能为空"),
        Map.entry("Material loan is not pending return", "材料借阅当前不是待归还状态"),
        Map.entry("Material loan not found", "材料借阅记录不存在"),
        Map.entry("Medical order cannot be cancelled", "医嘱无法取消"),
        Map.entry("Medical order category code already exists", "医嘱分类编码已存在"),
        Map.entry("Medical order category not found", "医嘱分类不存在"),
        Map.entry("Medical order category still has children or items", "医嘱分类下仍有子分类或项目"),
        Map.entry("Medical order is assigned to another executor", "医嘱已分配给其他执行人"),
        Map.entry("Medical order is not in progress", "医嘱未处于执行中状态"),
        Map.entry("Medical order is not pending", "医嘱未处于待处理状态"),
        Map.entry("Medical order item code already exists", "医嘱项目编码已存在"),
        Map.entry("Medical order item is referenced by charges or packages", "医嘱项目已被收费项或套餐引用"),
        Map.entry("Medical order item not found", "医嘱项目不存在"),
        Map.entry("Medical order not found", "医嘱不存在"),
        Map.entry("Numbering rule not found", "编号规则不存在"),
        Map.entry("Object does not belong to case", "对象不属于当前病例"),
        Map.entry("Only image files can be uploaded", "只能上传图片文件"),
        Map.entry("Only one primary role is allowed", "只允许设置一个主角色"),
        Map.entry("Operation is not allowed", "当前操作不被允许"),
        Map.entry("Package code already exists", "套餐编码已存在"),
        Map.entry("Package not found", "套餐不存在"),
        Map.entry("Package not found after create", "创建后未找到套餐"),
        Map.entry("Participant does not belong to consultation", "参与人不属于当前会诊"),
        Map.entry("Pathology case identifier is required", "病例标识不能为空"),
        Map.entry("Pathology case not found", "病例不存在"),
        Map.entry("Pathology report not found", "病理报告不存在"),
        Map.entry("Patient id or patient name is required", "患者证件号或患者姓名不能为空"),
        Map.entry("Patient id or patient name must be provided", "患者证件号或患者姓名不能为空"),
        Map.entry("Pending revision request already exists", "待处理的修订申请已存在"),
        Map.entry("Permission denied", "没有权限执行该操作"),
        Map.entry("Quality check result is required", "质检结果不能为空"),
        Map.entry("Reagent code already exists", "试剂编码已存在"),
        Map.entry("Reagent not found", "试剂不存在"),
        Map.entry("Reagent stock batch already exists", "试剂库存批次已存在"),
        Map.entry("Reagent stock not found", "试剂库存不存在"),
        Map.entry("Receipt items are required", "接收项目不能为空"),
        Map.entry("Receipt specimen does not belong to the same application", "接收标本不属于同一申请单"),
        Map.entry("Received specimens must pass quality check", "已接收标本必须通过质检"),
        Map.entry("Rejected or returned specimens must provide a reason", "拒收或退回标本必须提供原因"),
        Map.entry("Report cannot be rejected", "报告无法驳回"),
        Map.entry("Report cannot request revision", "报告当前无法发起修订"),
        Map.entry("Report is not editable draft", "报告不是可编辑的草稿"),
        Map.entry("Report is not reviewed", "报告未审核"),
        Map.entry("Report is not signed", "报告未签发"),
        Map.entry("Report is not submitted", "报告未提交"),
        Map.entry("Request body is required and must be valid JSON", "请求体不能为空，且必须是合法的 JSON"),
        Map.entry("Request validation failed", "请求参数校验失败"),
        Map.entry("Resource conflict", "资源冲突"),
        Map.entry("Resource not found", "资源不存在"),
        Map.entry("Revision request is not pending", "修订申请未处于待处理状态"),
        Map.entry("Revision request not found", "修订申请不存在"),
        Map.entry("Role code already exists", "角色编码已存在"),
        Map.entry("Role is still assigned to users", "角色仍分配给用户"),
        Map.entry("Role not found", "角色不存在"),
        Map.entry("Sampling block not found", "取材块不存在"),
        Map.entry("Sampling template not found", "取材模板不存在"),
        Map.entry("Sequence length must be between 1 and 12", "序列长度必须在1到12之间"),
        Map.entry("Slide not found", "玻片不存在"),
        Map.entry("Specimen already received", "标本已接收"),
        Map.entry("Specimen barcode already exists", "标本条码已存在"),
        Map.entry("Specimen barcode not found", "标本条码不存在"),
        Map.entry("Specimen does not belong to application", "标本不属于当前申请单"),
        Map.entry("Specimen does not belong to transport order", "标本不属于当前转运单"),
        Map.entry("Specimen must be fixed before direct receipt", "标本必须先固定后才能直接接收"),
        Map.entry("Specimen must be fixed before transport", "标本必须先固定后才能转运"),
        Map.entry("Specimen not found", "标本不存在"),
        Map.entry("Successful login requires user id", "成功登录时必须提供用户ID"),
        Map.entry("Task does not belong to case", "任务不属于当前病例"),
        Map.entry("Task object mismatch", "任务对象不匹配"),
        Map.entry("Technical marking skipped", "技术标记已跳过"),
        Map.entry("Technical task is completed", "技术任务已完成"),
        Map.entry("Technical task is not active", "技术任务未处于激活状态"),
        Map.entry("Technical task not found", "技术任务不存在"),
        Map.entry("Technical task type mismatch", "技术任务类型不匹配"),
        Map.entry("Template category code already exists", "模板分类编码已存在"),
        Map.entry("Template category not found", "模板分类不存在"),
        Map.entry("Template category still has children or templates", "模板分类下仍有子分类或模板"),
        Map.entry("Template code already exists", "模板编码已存在"),
        Map.entry("Template not found", "模板不存在"),
        Map.entry("Transport order is not ready for receipt", "转运单未准备好接收"),
        Map.entry("Transport order not found", "转运单不存在"),
        Map.entry("Unsupported quality check result", "不支持的质检结果"),
        Map.entry("Unsupported technical task priority", "不支持的技术任务优先级"),
        Map.entry("User code, job number or login tag code already exists", "用户编码、工号或登录标识编码已存在"),
        Map.entry("User is not assigned to diagnostic task", "用户未被分配到诊断任务"),
        Map.entry("User is not assigned to publish the report", "用户未被分配发布报告"),
        Map.entry("User is not assigned to review the report", "用户未被分配审核报告"),
        Map.entry("User is not consultation host", "用户不是会诊主持人"),
        Map.entry("User is not invited to consultation", "用户未被邀请参与会诊"),
        Map.entry("User login name or code already exists", "用户登录名或编码已存在"),
        Map.entry("User not found", "用户不存在"),
        Map.entry("must be a well-formed email address", "邮箱格式不正确"),
        Map.entry("must be greater than or equal to 1", "必须大于或等于1"),
        Map.entry("must not be blank", "不能为空"),
        Map.entry("must not be empty", "不能为空"),
        Map.entry("must not be null", "不能为空"),
        Map.entry("size must be between 0 and 64", "长度必须在0到64之间")
    );

    private static final Map<String, String> FIELD_LABEL_TRANSLATIONS = Map.ofEntries(
        Map.entry("Application type", "申请类型"),
        Map.entry("Application number", "申请单号"),
        Map.entry("Assigned name", "指派人姓名"),
        Map.entry("Assigned user", "指派用户"),
        Map.entry("Avatar", "头像"),
        Map.entry("Category code", "分类编码"),
        Map.entry("Category id", "分类ID"),
        Map.entry("Category name", "分类名称"),
        Map.entry("Category type", "分类类型"),
        Map.entry("Charge item code", "收费项目编码"),
        Map.entry("Charge item name", "收费项目名称"),
        Map.entry("Clinical diagnosis", "临床诊断"),
        Map.entry("Config key", "配置键"),
        Map.entry("Config name", "配置名称"),
        Map.entry("Data scope", "数据范围"),
        Map.entry("Default content", "默认内容"),
        Map.entry("Department id", "科室ID"),
        Map.entry("Department name", "科室名称"),
        Map.entry("Email", "邮箱"),
        Map.entry("Execution scope", "执行范围"),
        Map.entry("Job number", "工号"),
        Map.entry("Login name", "登录名"),
        Map.entry("Login tag code", "登录标识编码"),
        Map.entry("Order dict item id", "医嘱字典项ID"),
        Map.entry("Order item code", "医嘱项目编码"),
        Map.entry("Order item name", "医嘱项目名称"),
        Map.entry("Order type", "医嘱类型"),
        Map.entry("Package code", "套餐编码"),
        Map.entry("Package items", "套餐项目"),
        Map.entry("Package name", "套餐名称"),
        Map.entry("Package type", "套餐类型"),
        Map.entry("Part alias", "部位别名"),
        Map.entry("Part code", "部位编码"),
        Map.entry("Part level", "部位层级"),
        Map.entry("Part name", "部位名称"),
        Map.entry("Password", "密码"),
        Map.entry("Phone", "手机号"),
        Map.entry("Remarks", "备注"),
        Map.entry("Role code", "角色编码"),
        Map.entry("Role id", "角色ID"),
        Map.entry("Role name", "角色名称"),
        Map.entry("Role type", "角色类型"),
        Map.entry("Specification", "规格"),
        Map.entry("Specimen site", "标本部位"),
        Map.entry("Submitting department id", "送检科室ID"),
        Map.entry("Submitting department name", "送检科室名称"),
        Map.entry("Submitting doctor name", "送检医生姓名"),
        Map.entry("Submitting doctor user id", "送检医生用户ID"),
        Map.entry("Technical workflow node code", "技术流程节点编码"),
        Map.entry("Title name", "职称名称"),
        Map.entry("Unit", "单位"),
        Map.entry("User code", "用户编码"),
        Map.entry("User name", "用户名"),
        Map.entry("Value type", "值类型")
    );

    private HttpMessageTranslator() {
    }

    public static String translate(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }
        String trimmed = message.trim();
        String exact = EXACT_TRANSLATIONS.get(trimmed);
        if (exact != null) {
            return exact;
        }
        String segmented = translateSegmentedMessage(trimmed);
        if (segmented != null) {
            return segmented;
        }

        Matcher matcher = NO_PERMISSION_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return "用户没有权限：" + matcher.group(1);
        }
        matcher = REQUIRED_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return translatedField(matcher.group(1)) + "不能为空";
        }
        matcher = NOT_BLANK_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return translatedField(matcher.group(1)) + "不能为空";
        }
        matcher = NOT_EMPTY_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return translatedField(matcher.group(1)) + "不能为空";
        }
        matcher = NOT_EXCEED_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return translatedField(matcher.group(1)) + "长度不能超过" + matcher.group(2) + "个字符";
        }
        matcher = NOT_NEGATIVE_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return translatedField(matcher.group(1)) + "不能为负数";
        }
        matcher = BETWEEN_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return translatedField(matcher.group(1)) + "必须在" + matcher.group(2) + "到" + matcher.group(3) + "之间";
        }
        matcher = UNSUPPORTED_WITH_VALUE_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            return "不支持的" + translatedField(matcher.group(1)) + "：" + matcher.group(2);
        }
        return trimmed;
    }

    private static String translateSegmentedMessage(String message) {
        if (message.contains("; ")) {
            String[] parts = message.split("; ");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = translate(parts[i]);
            }
            return String.join("; ", parts);
        }

        Matcher matcher = CONSTRAINT_WITH_PATH_PATTERN.matcher(message);
        if (matcher.matches() && !message.startsWith("http")) {
            String translatedTail = translate(matcher.group(2));
            if (!translatedTail.equals(matcher.group(2))) {
                return matcher.group(1) + ": " + translatedTail;
            }
        }
        return null;
    }

    private static String translatedField(String field) {
        String normalized = field == null ? "" : field.trim();
        return FIELD_LABEL_TRANSLATIONS.getOrDefault(normalized, normalized);
    }
}
