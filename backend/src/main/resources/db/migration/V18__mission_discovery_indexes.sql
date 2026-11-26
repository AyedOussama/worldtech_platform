-- Discovery filters by normalized skill before applying the bounded page.
CREATE INDEX mission_skill_filter_idx ON app.mission_skill(lower(skill), account_id, mission_id);
