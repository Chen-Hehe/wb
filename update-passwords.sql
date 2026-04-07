USE weibo_db;
UPDATE users SET password = '$2a$10$zEB207OnCuE9yzD8ysDVMuMAscvc5.LfmUSx4KV1Up6WZDZXUFhim' WHERE username IN ('admin', 'user1', 'user2', 'user3', 'user4');
SELECT username, LEFT(password, 40) AS pwd FROM users ORDER BY id;
