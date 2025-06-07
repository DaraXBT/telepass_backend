CREATE TABLE users (
  user_id SERIAL PRIMARY KEY,
  username VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL CHECK (role IN ('admin', 'organizer'))
);

CREATE TABLE event_organizers (
  assign_id SERIAL PRIMARY KEY,
  user_id INT NOT NULL UNIQUE REFERENCES users(user_id),
  admin_id INT NOT NULL REFERENCES users(user_id),
  name VARCHAR(100) NOT NULL
);

CREATE TABLE events (
  event_id SERIAL PRIMARY KEY,
  event_name VARCHAR(150) NOT NULL,
  event_datetime TIMESTAMP,
  event_location VARCHAR(200),
  event_status VARCHAR(20) NOT NULL CHECK (event_status IN ('planned', 'ongoing', 'completed', 'cancelled')),
  public_qr_code VARCHAR UNIQUE
);

CREATE TABLE event_organizer_assignments (
  event_id INT NOT NULL REFERENCES events(event_id),
  organizer_id INT NOT NULL REFERENCES event_organizers(assign_id),
  assigned_at TIMESTAMP,
  PRIMARY KEY (event_id, organizer_id)
);

CREATE TABLE participants (
  participant_id SERIAL PRIMARY KEY,
  full_name VARCHAR(100) NOT NULL,
  gender VARCHAR(10) NOT NULL,
  email VARCHAR(100) NOT NULL UNIQUE,
  phone VARCHAR(15),
  job VARCHAR(100)
);

CREATE TABLE registrations (
  registration_id SERIAL PRIMARY KEY,
  participant_id INT NOT NULL REFERENCES participants(participant_id),
  event_id INT NOT NULL REFERENCES events(event_id),
  qr_code VARCHAR(255) NOT NULL UNIQUE,
  registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  check_in_time TIMESTAMP,
  check_out_time TIMESTAMP,
  status VARCHAR(20) DEFAULT 'registered' CHECK (status IN ('registered', 'checked_in', 'cancelled'))
);

CREATE TABLE event_reports (
  report_id SERIAL PRIMARY KEY,
  event_id INT NOT NULL UNIQUE REFERENCES events(event_id),
  attendance_count INT NOT NULL
);
