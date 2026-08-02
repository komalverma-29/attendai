-- V12: Add FK constraint from users.person_id → persons.id
--
-- This constraint could not be added in V3 (create_users_table) because
-- the persons table did not exist at that point in the migration sequence.
-- Now that persons exists (V11), we add the referential integrity constraint.

ALTER TABLE users
    ADD CONSTRAINT fk_users_person
    FOREIGN KEY (person_id) REFERENCES persons (id);
