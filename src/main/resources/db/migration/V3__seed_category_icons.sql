SELECT category_name,
       emoji,
       icon_url
FROM category_icons
ORDER BY id;

INSERT INTO category_icons (category_name, emoji, icon_url, created_at)
VALUES ('Housing', '🏠 ', 'https://cdn.example.com/icons/housing.png', CURRENT_TIMESTAMP),
       ('Food', '🍽 ', 'https://cdn.example.com/icons/food.png', CURRENT_TIMESTAMP),
       ('Shopping', '🛍 ', 'https://cdn.example.com/icons/shopping.png', CURRENT_TIMESTAMP),
       ('Salary', '💵 ', 'https://cdn.example.com/icons/salary.png', CURRENT_TIMESTAMP),
       ('Freelance', '💼 ', 'https://cdn.example.com/icons/freelance.png', CURRENT_TIMESTAMP),
       ('Investments', '📈 ', 'https://cdn.example.com/icons/investments.png', CURRENT_TIMESTAMP),
       ('Other', '👤 ', 'https://cdn.example.com/icons/other.png', CURRENT_TIMESTAMP),
       ('Transportation', '🚗 ', 'https://cdn.example.com/icons/transportation.png', CURRENT_TIMESTAMP),
       ('Entertainment', '🎮 ', 'https://cdn.example.com/icons/entertainment.png', CURRENT_TIMESTAMP),
       ('Health', '🏥 ', 'https://cdn.example.com/icons/health.png', CURRENT_TIMESTAMP) ON DUPLICATE KEY
UPDATE
    category_name = category_name;

INSERT INTO category_icons (category_name, emoji, icon_url, created_at)
VALUES ('Education', '🎓 ', 'https://cdn.example.com/icons/education.png', CURRENT_TIMESTAMP),
       ('Gifts', '🎁 ', 'https://cdn.example.com/icons/gifts.png', CURRENT_TIMESTAMP) ON DUPLICATE KEY
UPDATE
    category_name = category_name;
