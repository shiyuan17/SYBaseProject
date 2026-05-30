update stat_report_templates
set parameter_schema = '{"from":"datetime","to":"datetime","workloadUserId":"string"}'
where template_code = 'TPL_WORKLOAD_OVERVIEW';
