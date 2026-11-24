-- 完整版保留旧任务，但 NULL 归属不会自动分给任何注册用户。
-- 注册自己的账号后，先用 SELECT 核对账号和原任务。
SELECT id, username, nickname FROM learnflow.app_user;
SELECT id, title, user_id FROM learnflow.learning_task WHERE user_id IS NULL;
-- 仅在确认正确账号与任务 id 后，手动填写并执行：
-- UPDATE learnflow.learning_task SET user_id = <你的账号id>
-- WHERE user_id IS NULL AND id IN (<需要导入的任务id列表>);
