UPDATE numbering_rules
SET scope_type = 'GLOBAL',
    updated_at = CURRENT_TIMESTAMP
WHERE biz_type = 'BLOCK_NO'
  AND scope_type <> 'GLOBAL';
