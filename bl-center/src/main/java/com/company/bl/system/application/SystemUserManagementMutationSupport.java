package com.company.bl.system.application;

import com.company.bl.domain.enums.BlErrorCode;
import com.company.bl.domain.exception.BlBusinessException;
import com.company.bl.support.application.NumberingService;
import com.company.bl.support.application.OperationAuditService;
import com.company.bl.system.infrastructure.SystemJdbcRepository;
import com.company.bl.system.infrastructure.SystemRoleJdbcRepository;
import com.company.bl.system.infrastructure.SystemUserJdbcRepository;
import com.company.common.security.crypto.Sm3PasswordEncoder;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
class SystemUserManagementMutationSupport extends AbstractSystemUserManagementSupport {

    private final NumberingService numberingService;
    private final OperationAuditService operationAuditService;
    private final Sm3PasswordEncoder sm3PasswordEncoder;

    SystemUserManagementMutationSupport(SystemUserJdbcRepository systemUserJdbcRepository,
                                        NumberingService numberingService,
                                        OperationAuditService operationAuditService,
                                        Sm3PasswordEncoder sm3PasswordEncoder) {
        super(systemUserJdbcRepository);
        this.numberingService = numberingService;
        this.operationAuditService = operationAuditService;
        this.sm3PasswordEncoder = sm3PasswordEncoder;
    }

    @Transactional
    public SystemManagementService.UserView createUser(SystemManagementService.CreateUserCommand command) {
        String userCode = resolveCreateCode(command.userCode(), numberingService::generateUserCode);
        String loginTagCode = resolveCreateCode(command.loginTagCode(), numberingService::generateLoginTagCode);
        return operationAuditService.audit("SYSTEM", "USER", "create_user", () -> {
            try {
                EncodedPassword encodedPassword = encodePassword(command.password());
                SystemJdbcRepository.UserRow user = systemUserJdbcRepository.insertUser(new SystemJdbcRepository.CreateUserRow(
                    "USER-" + UUID.randomUUID(),
                    userCode,
                    command.loginName(),
                    command.name(),
                    encodedPassword.password(),
                    encodedPassword.passwordAlgo(),
                    encodedPassword.passwordSalt(),
                    null,
                    command.jobNo(),
                    command.titleName(),
                    command.departmentId(),
                    command.departmentName(),
                    command.phone(),
                    command.email(),
                    command.avatar(),
                    loginTagCode,
                    command.enabled(),
                    LocalDateTime.now(),
                    LocalDateTime.now()));
                return toUserView(user, List.of());
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "User login name or code already exists");
            }
        }, SystemManagementService.UserView::id, () -> command.loginName());
    }

    @Transactional
    public SystemManagementService.UserView updateUser(String userId, SystemManagementService.UpdateUserCommand command) {
        return operationAuditService.audit("SYSTEM", "USER", "update_user", () -> {
            SystemJdbcRepository.UserRow current = ensureUserExists(userId);
            String userCode = resolveExistingCode(command.userCode(), current.userCode(), "User code");
            String loginTagCode = resolveExistingCode(command.loginTagCode(), current.loginTagCode(), "Login tag code");
            try {
                systemUserJdbcRepository.updateUser(userId, new SystemJdbcRepository.UpdateUserRow(
                    userCode,
                    command.name(),
                    command.jobNo(),
                    command.titleName(),
                    command.departmentId(),
                    command.departmentName(),
                    command.phone(),
                    command.email(),
                    command.avatar(),
                    loginTagCode,
                    command.enabled()));
            } catch (DataAccessException exception) {
                throw new BlBusinessException(BlErrorCode.RESOURCE_CONFLICT, 409, "User code, job number or login tag code already exists");
            }
            SystemJdbcRepository.UserRow user = ensureUserExists(userId);
            List<SystemRoleJdbcRepository.RoleAssignmentRow> assignments =
                systemUserJdbcRepository.findUserRoleAssignments(List.of(userId)).getOrDefault(userId, List.of());
            return toUserView(user, assignments);
        }, SystemManagementService.UserView::id, () -> userId);
    }

    @Transactional
    public SystemManagementService.UserView updateUserEnabled(String userId, boolean enabled) {
        return operationAuditService.audit("SYSTEM", "USER", "update_user_enabled", () -> {
            ensureUserExists(userId);
            systemUserJdbcRepository.updateUserEnabled(userId, enabled);
            SystemJdbcRepository.UserRow user = ensureUserExists(userId);
            List<SystemRoleJdbcRepository.RoleAssignmentRow> assignments =
                systemUserJdbcRepository.findUserRoleAssignments(List.of(userId)).getOrDefault(userId, List.of());
            return toUserView(user, assignments);
        }, SystemManagementService.UserView::id, () -> userId + ":" + enabled);
    }

    @Transactional
    public SystemManagementService.UserView assignUserRoles(String userId, SystemManagementService.AssignUserRolesCommand command) {
        return operationAuditService.audit("SYSTEM", "USER_ROLE", "assign_user_roles", () -> {
            ensureUserExists(userId);
            long primaryCount = command.assignments().stream().filter(SystemManagementService.RoleAssignmentInput::primary).count();
            if (primaryCount > 1) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Only one primary role is allowed");
            }
            systemUserJdbcRepository.replaceUserRoles(userId, command.assignments().stream()
                .map(input -> new SystemJdbcRepository.UserRoleAssignmentCommand(input.roleId(), input.primary()))
                .toList());
            SystemJdbcRepository.UserRow user = ensureUserExists(userId);
            List<SystemRoleJdbcRepository.RoleAssignmentRow> assignments =
                systemUserJdbcRepository.findUserRoleAssignments(List.of(userId)).getOrDefault(userId, List.of());
            return toUserView(user, assignments);
        }, SystemManagementService.UserView::id, () -> userId);
    }

    @Transactional
    public void recordUserLogin(SystemManagementService.RecordUserLoginCommand command) {
        LocalDateTime loginAt = command.loginAt() == null ? LocalDateTime.now() : command.loginAt();
        String loginResult = normalizeLoginResult(command.loginResult());
        String userId = blankToNull(command.userId());
        if ("SUCCESS".equals(loginResult)) {
            if (userId == null) {
                throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Successful login requires user id");
            }
            ensureUserExists(userId);
            systemUserJdbcRepository.updateUserLastLogin(userId, loginAt, command.clientIp(), command.clientDevice());
        } else if (userId != null) {
            ensureUserExists(userId);
        }

        systemUserJdbcRepository.insertUserLoginLog(new SystemJdbcRepository.CreateUserLoginLogRow(
            "ULL-" + UUID.randomUUID(),
            userId,
            command.loginName(),
            loginResult,
            command.clientIp(),
            command.clientDevice(),
            loginAt,
            null,
            command.failureReason(),
            command.remarks()));
    }

    private String normalizeLoginResult(String loginResult) {
        if (loginResult == null || loginResult.isBlank()) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Login result must not be blank");
        }
        String normalized = loginResult.trim().toUpperCase();
        if (!"SUCCESS".equals(normalized) && !"FAILED".equals(normalized)) {
            throw new BlBusinessException(BlErrorCode.INVALID_ARGUMENT, 400, "Login result must be SUCCESS or FAILED");
        }
        return normalized;
    }

    private String resolveCreateCode(String requestedCode, Supplier<String> generator) {
        String normalizedCode = blankToNull(requestedCode);
        return normalizedCode == null ? generator.get() : normalizedCode;
    }

    private String resolveExistingCode(String requestedCode, String existingCode, String fieldLabel) {
        String normalizedCode = blankToNull(requestedCode);
        if (normalizedCode == null || normalizedCode.equals(existingCode)) {
            return existingCode;
        }
        throw new BlBusinessException(
            BlErrorCode.INVALID_ARGUMENT,
            400,
            fieldLabel + " cannot be changed once created");
    }

    private EncodedPassword encodePassword(String rawPassword) {
        String normalizedPassword = blankToNull(rawPassword);
        if (normalizedPassword == null) {
            return new EncodedPassword(null, null, null);
        }
        String salt = sm3PasswordEncoder.generateSalt();
        return new EncodedPassword(
            sm3PasswordEncoder.encode(normalizedPassword, salt),
            Sm3PasswordEncoder.PASSWORD_ALGO_SM3,
            salt);
    }

    private record EncodedPassword(String password, String passwordAlgo, String passwordSalt) {
    }
}
