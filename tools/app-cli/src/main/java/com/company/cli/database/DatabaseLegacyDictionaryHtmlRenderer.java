package com.company.cli.database;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
class DatabaseLegacyDictionaryHtmlRenderer {

    String render(DatabaseDictionaryReport report) {
        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html lang="zh">
            <head>
              <meta charset="UTF-8">
              <title>Markdown TOC Example</title>
              <style>
                * { list-style: none;  box-sizing: border-box;}
                .toc { height: 100%;}
                table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }
                th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }
                h1, h2, h3, h4, h5, h6 { color: #2c3e50; }
                li { white-space:  nowrap }
                a { color: #3498db; text-decoration: none; }
                html , body { width: 100%; height: 100%; margin: 0; padding: 0; }
                .left { display: inline-block; width: 25%; height: 100%; overflow: auto; background: #f9f9f9; border: 1px solid #ddd; padding: 10px }
                .right { display: inline-block; flex-grow: 1; height: 100%; padding: 10px; overflow: auto; }
                #resizer { width: 5px; background: transparent; cursor: ew-resize;}
                #resizer:hover { background: blue; }
              </style>
            </head>
            <body style='display: flex'>
            """);
        html.append("<span class='left'><nav class=\"toc\"><ul>");
        html.append("<li><a href=\"#1-%E6%95%B0%E6%8D%AE%E5%BA%93%E8%A7%A3%E6%9E%90\">1 数据库解析 </a><ul>");
        html.append("<li><a href=\"#1.1-%E8%A1%A8%E6%B8%85%E5%8D%95\">1.1 表清单 </a></li>");
        html.append("<li><a href=\"#1.2-%E8%A1%A8%E5%AD%97%E6%AE%B5%E6%98%8E%E7%BB%86\">1.2 表字段明细 </a></li>");
        html.append("<li><a href=\"#1.3-%E5%85%B3%E7%B3%BB%E5%9B%BE\">1.3 关系图 </a></li>");
        html.append("</ul></li>");

        int ownerIndex = 4;
        for (DatabaseSourceReport source : report.sources()) {
            for (DatabaseOwnerReport owner : source.owners()) {
                appendOwnerToc(html, owner, ownerIndex++);
            }
        }

        html.append("</ul></nav></span>");
        html.append("<div id=\"resizer\"></div>");
        html.append("<span class='right'>");
        html.append("<h1 id=\"1-%E6%95%B0%E6%8D%AE%E5%BA%93%E8%A7%A3%E6%9E%90\" tabindex=\"-1\">1 数据库解析 <a class=\"header-anchor\" href=\"#1-%E6%95%B0%E6%8D%AE%E5%BA%93%E8%A7%A3%E6%9E%90\" aria-hidden=\"true\">#</a></h1>");
        html.append("<h2 id=\"1.1-%E8%A1%A8%E6%B8%85%E5%8D%95\" tabindex=\"-1\">1.1 表清单 <a class=\"header-anchor\" href=\"#1.1-%E8%A1%A8%E6%B8%85%E5%8D%95\" aria-hidden=\"true\">#</a></h2>");
        html.append("<h2 id=\"1.2-%E8%A1%A8%E5%AD%97%E6%AE%B5%E6%98%8E%E7%BB%86\" tabindex=\"-1\">1.2 表字段明细 <a class=\"header-anchor\" href=\"#1.2-%E8%A1%A8%E5%AD%97%E6%AE%B5%E6%98%8E%E7%BB%86\" aria-hidden=\"true\">#</a></h2>");
        html.append("<h2 id=\"1.3-%E5%85%B3%E7%B3%BB%E5%9B%BE\" tabindex=\"-1\">1.3 关系图 <a class=\"header-anchor\" href=\"#1.3-%E5%85%B3%E7%B3%BB%E5%9B%BE\" aria-hidden=\"true\">#</a></h2>");

        ownerIndex = 4;
        for (DatabaseSourceReport source : report.sources()) {
            for (DatabaseOwnerReport owner : source.owners()) {
                appendOwnerBody(html, owner, ownerIndex++);
            }
        }

        html.append("</span>");
        html.append("""
              <script>
                document.addEventListener('DOMContentLoaded', function() {
                    const leftPanel = document.querySelector('.left');
                    const rightPanel = document.querySelector('.right');
                    const divider = document.getElementById('resizer');
                    const container = document.querySelector('body');

                    let isDragging = false;
                    let containerLeft, containerWidth;

                    divider.addEventListener('mousedown', function(e) {
                        isDragging = true;
                        containerLeft = container.getBoundingClientRect().left;
                        containerWidth = container.offsetWidth;
                        document.body.style.cursor = 'col-resize';
                        leftPanel.style.userSelect = 'none';
                        leftPanel.style.pointerEvents = 'none';
                        rightPanel.style.userSelect = 'none';
                        rightPanel.style.pointerEvents = 'none';
                    });

                    document.addEventListener('mousemove', function(e) {
                        if (!isDragging) return;

                        const x = e.clientX - containerLeft;
                        const percent = Math.min(Math.max((x / containerWidth) * 100, 15), 85);

                        leftPanel.style.flex = '0 0 ' + percent + '%';
                    });

                    document.addEventListener('mouseup', function() {
                        if (isDragging) {
                            isDragging = false;
                            document.body.style.cursor = '';
                            leftPanel.style.userSelect = '';
                            leftPanel.style.pointerEvents = '';
                            rightPanel.style.userSelect = '';
                            rightPanel.style.pointerEvents = '';

                            leftPanel.style.transition = 'flex 0.3s ease';
                            setTimeout(() => { leftPanel.style.transition = ''; }, 300);
                        }
                    });
                });
              </script>
            </body>
            </html>
            """);
        return html.toString();
    }

    private void appendOwnerToc(StringBuilder html, DatabaseOwnerReport owner, int ownerIndex) {
        String ownerId = ownerId(owner.owner(), ownerIndex);
        html.append("<li><a href=\"#").append(ownerId).append("\">1.")
            .append(ownerIndex).append(" ").append(escapeHtml(owner.owner())).append(" </a><ul>");
        html.append("<li><a href=\"#").append(ownerSectionId(owner.owner(), "table-list"))
            .append("\">1.").append(ownerIndex).append(".1 表清单 </a></li>");
        html.append("<li><a href=\"#").append(ownerSectionId(owner.owner(), "column-list"))
            .append("\">1.").append(ownerIndex).append(".2 表字段明细 </a><ul>");
        for (int i = 0; i < owner.tables().size(); i++) {
            DatabaseTableReport table = owner.tables().get(i);
            html.append("<li><a href=\"#").append(tableHeadingId(ownerIndex, i + 1, table)).append("\">1.")
                .append(ownerIndex).append(".2.").append(i + 1).append(" ")
                .append(escapeHtml(table.tableName())).append("[")
                .append(escapeHtml(displayTableName(table))).append("] </a></li>");
        }
        html.append("</ul></li>");
        html.append("<li><a href=\"#").append(ownerSectionId(owner.owner(), "relation"))
            .append("\">1.").append(ownerIndex).append(".3 关系图 </a></li>");
        html.append("</ul></li>");
    }

    private void appendOwnerBody(StringBuilder html, DatabaseOwnerReport owner, int ownerIndex) {
        html.append("<h1 id=\"").append(ownerId(owner.owner(), ownerIndex)).append("\" tabindex=\"-1\">1.")
            .append(ownerIndex).append(" ").append(escapeHtml(owner.owner()))
            .append(" <a class=\"header-anchor\" href=\"#").append(ownerId(owner.owner(), ownerIndex))
            .append("\" aria-hidden=\"true\">#</a></h1>");

        html.append("<h2 id=\"").append(ownerSectionId(owner.owner(), "table-list"))
            .append("\" tabindex=\"-1\">1.").append(ownerIndex)
            .append(".1 表清单 <a class=\"header-anchor\" href=\"#")
            .append(ownerSectionId(owner.owner(), "table-list")).append("\" aria-hidden=\"true\">#</a></h2>");
        appendTableList(html, owner.tables());

        html.append("<h2 id=\"").append(ownerSectionId(owner.owner(), "column-list"))
            .append("\" tabindex=\"-1\">1.").append(ownerIndex)
            .append(".2 表字段明细 <a class=\"header-anchor\" href=\"#")
            .append(ownerSectionId(owner.owner(), "column-list")).append("\" aria-hidden=\"true\">#</a></h2>");
        for (int i = 0; i < owner.tables().size(); i++) {
            appendTableDetails(html, ownerIndex, i + 1, owner.tables().get(i));
        }

        html.append("<h2 id=\"").append(ownerSectionId(owner.owner(), "relation"))
            .append("\" tabindex=\"-1\">1.").append(ownerIndex)
            .append(".3 关系图 <a class=\"header-anchor\" href=\"#")
            .append(ownerSectionId(owner.owner(), "relation")).append("\" aria-hidden=\"true\">#</a></h2>");
    }

    private void appendTableList(StringBuilder html, List<DatabaseTableReport> tables) {
        html.append("<table><thead><tr><th>数据表</th><th>名称</th><th>备注说明</th></tr></thead><tbody>");
        for (DatabaseTableReport table : tables) {
            html.append("<tr><td>").append(escapeHtml(table.tableName())).append("</td>")
                .append("<td>").append(escapeHtml(displayTableName(table))).append("</td>")
                .append("<td>").append(escapeHtml(displayTableComment(table))).append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendTableDetails(StringBuilder html, int ownerIndex, int tableIndex, DatabaseTableReport table) {
        String headingId = tableHeadingId(ownerIndex, tableIndex, table);
        html.append("<h3 id=\"").append(headingId).append("\" tabindex=\"-1\">1.")
            .append(ownerIndex).append(".2.").append(tableIndex).append(" ")
            .append(escapeHtml(table.tableName())).append("[")
            .append(escapeHtml(displayTableName(table))).append("] <a class=\"header-anchor\" href=\"#")
            .append(headingId).append("\" aria-hidden=\"true\">#</a></h3>");
        html.append("<table><thead><tr>")
            .append("<th>代码</th><th>名称</th><th>主键</th><th>不为空</th><th>自增</th><th>业务数据类型</th>")
            .append("<th>数据类型</th><th>长度</th><th>小数点</th><th>默认值</th><th>备注</th>")
            .append("<th>代码值标准</th><th>数据项标准</th><th>贯标要求</th><th>贯标级别</th>")
            .append("</tr></thead><tbody>");
        for (DatabaseColumnReport column : table.columns()) {
            ColumnTypeParts typeParts = ColumnTypeParts.parse(column.type());
            html.append("<tr>")
                .append("<td>").append(escapeHtml(column.name())).append("</td>")
                .append("<td>").append(escapeHtml(displayColumnName(column))).append("</td>")
                .append("<td>").append(column.primaryKey() ? "✓" : "").append("</td>")
                .append("<td>").append(column.nullable() ? "" : "✓").append("</td>")
                .append("<td></td><td></td>")
                .append("<td>").append(escapeHtml(typeParts.baseType())).append("</td>")
                .append("<td>").append(escapeHtml(orEmpty(typeParts.length()))).append("</td>")
                .append("<td>").append(escapeHtml(orEmpty(typeParts.scale()))).append("</td>")
                .append("<td>").append(escapeHtml(orEmpty(column.defaultValue()))).append("</td>")
                .append("<td>").append(escapeHtml(displayColumnComment(column))).append("</td>")
                .append("<td></td><td></td><td></td><td></td>")
                .append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private String ownerId(String owner, int ownerIndex) {
        return "1." + ownerIndex + "-" + sanitize(owner);
    }

    private String ownerSectionId(String owner, String suffix) {
        return sanitize(owner) + "-" + suffix;
    }

    private String tableHeadingId(int ownerIndex, int tableIndex, DatabaseTableReport table) {
        return "1." + ownerIndex + ".2." + tableIndex + "-" + sanitize(table.tableName()) + "%5B"
            + sanitize(displayTableName(table)) + "%5D";
    }

    private String displayTableName(DatabaseTableReport table) {
        return firstNonBlank(table.comment(), table.tableName());
    }

    private String displayTableComment(DatabaseTableReport table) {
        return firstNonBlank(table.comment(), table.tableName());
    }

    private String displayColumnName(DatabaseColumnReport column) {
        return firstNonBlank(column.comment(), column.name());
    }

    private String displayColumnComment(DatabaseColumnReport column) {
        return firstNonBlank(column.comment(), column.name());
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return orEmpty(fallback);
    }

    private String sanitize(String value) {
        return value.toLowerCase(Locale.ROOT).replace(" ", "-");
    }

    private String orEmpty(String value) {
        return value == null ? "" : value;
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private record ColumnTypeParts(String baseType, String length, String scale) {

        static ColumnTypeParts parse(String rawType) {
            if (rawType == null || rawType.isBlank()) {
                return new ColumnTypeParts("", "", "");
            }
            int left = rawType.indexOf('(');
            int right = rawType.indexOf(')');
            if (left < 0 || right < left) {
                return new ColumnTypeParts(rawType, "", "");
            }
            String baseType = rawType.substring(0, left);
            String[] segments = rawType.substring(left + 1, right).split(",");
            String length = segments.length > 0 ? segments[0].trim() : "";
            String scale = segments.length > 1 ? segments[1].trim() : "";
            return new ColumnTypeParts(baseType, length, scale);
        }
    }
}
