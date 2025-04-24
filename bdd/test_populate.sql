USE YCYW;

-- Ajout dans user_credentials
INSERT INTO user_credentials (id, email, password, role) VALUES
(1, 'support@ycyw.com', '$2a$10$eHwVnZzA6L81kCRVFVqCuOCkkuZcTfdjSRoAVboY9L1zvxtPYdKCu', 'AGENT'), -- Support-1!
(2, 'user@test.com', '$2a$10$VHDOnnY.LH0kKtWHDZpOruv/YJALH57u4H4YUIcKtCB/8yrGuDnVu', 'USER'); -- Test-1!

-- Ajout dans user_profiles
INSERT INTO user_profiles (id, user_id, first_name, last_name, company, type) VALUES
(1, 1, 'Sabrina', 'Yucayowich', 'YCYW', 'SUPPORT'),
(2, 2, 'Tom', 'Person', '', 'INDIVIDUAL');

-- Ajout dans dialogs
INSERT INTO dialogs (id, topic, status, created_at, closed_at) VALUES
(1, 'Modifier une date de départ', 'CLOSED', '2025-04-12T12:12:00', '2025-04-12T12:20:00');

-- Ajout dans messages
INSERT INTO messages (id, dialog_id, timestamp, is_read, content, sender_id) VALUES
(1, 1, '2025-04-12T12:12:00', TRUE, 'Je souhaite de l''aide pour modifier une date de départ', 2),
(2, 1, '2025-04-12T12:13:00', TRUE, 'Bonjour Tom, je suis Sabrina en charge de votre dossier AB123456. Pouvez-vous me donner plus de renseignements ?', 1),
(3, 1, '2025-04-12T12:14:00', TRUE, 'J''ai fait une erreur, la date de départ sera le 18/04 au lieu du 28/04', 2),
(4, 1, '2025-04-12T12:15:00', TRUE, 'Les modifications dans un délai supérieur à 48h ne sont pas un problème et n''engendrent aucun frais, je me charge de votre modification', 1),
(5, 1, '2025-04-12T12:16:00', TRUE, 'Merci, quelle est la démarche ?', 2),
(6, 1, '2025-04-12T12:17:00', TRUE, 'La modification a été effectuée sur votre réservation AB123456. La nouvelle date de départ est le 18/04. Avez-vous besoin d''autre renseignement ?', 1),
(7, 1, '2025-04-12T12:18:00', TRUE, 'Merci, c''est parfait.', 2);

-- Ajout dans rel_user_dialog
INSERT INTO rel_user_dialog (dialog_id, user_profile_id) VALUES
(1, 1),
(1, 2);
