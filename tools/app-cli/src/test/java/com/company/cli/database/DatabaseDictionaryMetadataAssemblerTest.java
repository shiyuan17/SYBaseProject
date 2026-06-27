package com.company.cli.database;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseDictionaryMetadataAssemblerTest {

    private final DatabaseDictionaryMetadataAssembler assembler = new DatabaseDictionaryMetadataAssembler();

    @Test
    void shouldAssembleColumnsConstraintsForeignKeysAndIndexes() {
        DatabaseSourceReport report = assembler.assemble(
            new DatabaseConnectionTarget(List.of("bl-center"), "dm.jdbc.driver.DmDriver", "jdbc:dm://127.0.0.1:5236", "SYSDBA", "Dm.2027.Pwd."),
            List.of(
                new TableRow("SYSDBA", "USERS", "用户表"),
                new TableRow("SYSDBA", "USER_ROLES", "用户角色关联表")),
            List.of(
                new ColumnRow("SYSDBA", "USERS", "ID", 1, "VARCHAR2", 64, null, null, false, null, "主键"),
                new ColumnRow("SYSDBA", "USERS", "EMAIL", 2, "VARCHAR2", 128, null, null, false, null, "邮箱"),
                new ColumnRow("SYSDBA", "USER_ROLES", "USER_ID", 1, "VARCHAR2", 64, null, null, false, null, null),
                new ColumnRow("SYSDBA", "USER_ROLES", "ROLE_ID", 2, "VARCHAR2", 64, null, null, false, null, null)),
            List.of(
                new ConstraintRow("SYSDBA", "USERS", "PK_USERS", "P", "ID", 1, null, null, null),
                new ConstraintRow("SYSDBA", "USERS", "UK_USERS_EMAIL", "U", "EMAIL", 1, null, null, null),
                new ConstraintRow("SYSDBA", "USER_ROLES", "FK_USER_ROLES_USER", "R", "USER_ID", 1, "SYSDBA", "USERS", "ID")),
            List.of(
                new IndexRow("SYSDBA", "USERS", "IDX_USERS_EMAIL", "NONUNIQUE", "NORMAL", "EMAIL", 1),
                new IndexRow("SYSDBA", "USER_ROLES", "IDX_USER_ROLES_USER", "NONUNIQUE", "NORMAL", "USER_ID", 1)));

        assertThat(report.owners()).hasSize(1);
        DatabaseOwnerReport owner = report.owners().get(0);
        assertThat(owner.tables()).hasSize(2);

        DatabaseTableReport usersTable = owner.tables().stream()
            .filter(table -> table.tableName().equals("USERS"))
            .findFirst()
            .orElseThrow();
        assertThat(usersTable.comment()).isEqualTo("用户表");
        assertThat(usersTable.columns()).extracting(DatabaseColumnReport::name).containsExactly("ID", "EMAIL");
        assertThat(usersTable.columns().get(0).primaryKey()).isTrue();
        assertThat(usersTable.columns().get(1).keyConstraintNames()).containsExactly("UK_USERS_EMAIL");
        assertThat(usersTable.uniqueConstraints()).extracting(DatabaseUniqueConstraintReport::name)
            .containsExactly("PK_USERS", "UK_USERS_EMAIL");
        assertThat(usersTable.indexes()).extracting(DatabaseIndexReport::name).containsExactly("IDX_USERS_EMAIL");

        DatabaseTableReport userRolesTable = owner.tables().stream()
            .filter(table -> table.tableName().equals("USER_ROLES"))
            .findFirst()
            .orElseThrow();
        assertThat(userRolesTable.foreignKeys()).hasSize(1);
        DatabaseForeignKeyReport foreignKey = userRolesTable.foreignKeys().get(0);
        assertThat(foreignKey.name()).isEqualTo("FK_USER_ROLES_USER");
        assertThat(foreignKey.columns()).containsExactly("USER_ID");
        assertThat(foreignKey.referencedOwner()).isEqualTo("SYSDBA");
        assertThat(foreignKey.referencedTable()).isEqualTo("USERS");
        assertThat(foreignKey.referencedColumns()).containsExactly("ID");
    }
}
