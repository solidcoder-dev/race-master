CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TYPE auth_user_role AS ENUM ('ADMIN', 'regular');
CREATE TYPE auth_user_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED');

CREATE TABLE profile (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100) NOT NULL,
                         nickname VARCHAR(100),
                         phone VARCHAR(30),
                         email VARCHAR(255) UNIQUE,
                         nationality VARCHAR(100),
                         birth_date DATE,
                         roles TEXT[],
                         created_at TIMESTAMP DEFAULT NOW(),
                         updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE rally (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       name VARCHAR(255) NOT NULL,
                       description TEXT,
                       start_date DATE NOT NULL,
                       end_date DATE NOT NULL,
                       published BOOLEAN NOT NULL DEFAULT FALSE,
                       is_public BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE stage (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       rally_id UUID NOT NULL REFERENCES rally(id) ON DELETE CASCADE,
                       name VARCHAR(255) NOT NULL,
                       date DATE NOT NULL,
                       roadbook_file_url TEXT,
                       created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE waypoint (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          stage_id UUID NOT NULL REFERENCES stage(id) ON DELETE CASCADE,
                          name VARCHAR(100) NOT NULL,
                          latitude DECIMAL(9,6) NOT NULL,
                          longitude DECIMAL(9,6) NOT NULL,
                          radius_meters INTEGER NOT NULL,
                          mandatory BOOLEAN NOT NULL DEFAULT TRUE,
                          sequence INT,
                          created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE competitor (
                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                            rally_id UUID NOT NULL REFERENCES rally(id) ON DELETE CASCADE,
                            name VARCHAR(255) NOT NULL,
                            vehicle_type VARCHAR(50) NOT NULL,
                            team_name VARCHAR(255),
                            category VARCHAR(100) NOT NULL,
                            subcategory VARCHAR(100),
                            created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE crew_member (
                                    competitor_id UUID NOT NULL REFERENCES competitor(id) ON DELETE CASCADE,
                                    profile_id UUID NOT NULL REFERENCES profile(id) ON DELETE CASCADE,
                                    PRIMARY KEY (competitor_id, profile_id)
);

CREATE TABLE competitor_stage_track (
                                        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                        competitor_id UUID NOT NULL REFERENCES competitor(id) ON DELETE CASCADE,
                                        stage_id UUID NOT NULL REFERENCES stage(id) ON DELETE CASCADE,
                                        track_file_url TEXT NOT NULL,
                                        upload_time TIMESTAMP NOT NULL DEFAULT NOW(),
                                        UNIQUE (competitor_id, stage_id)
);

CREATE TABLE waypoint_pass (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               competitor_stage_track_id UUID NOT NULL REFERENCES competitor_stage_track(id) ON DELETE CASCADE,
                               waypoint_id UUID NOT NULL REFERENCES waypoint(id) ON DELETE CASCADE,
                               passed BOOLEAN NOT NULL,
                               distance_meters DECIMAL(10,2) NOT NULL
);

CREATE TABLE stage_score (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             competitor_id UUID NOT NULL REFERENCES competitor(id) ON DELETE CASCADE,
                             stage_id UUID NOT NULL REFERENCES stage(id) ON DELETE CASCADE,
                             track_id UUID REFERENCES competitor_stage_track(id) ON DELETE SET NULL,
                             waypoints_passed INTEGER NOT NULL,
                             waypoints_missed INTEGER NOT NULL,
                             penalties_minutes INTEGER NOT NULL,
                             score DECIMAL(10,2) NOT NULL,
                             created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE general_score (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               competitor_id UUID NOT NULL REFERENCES competitor(id) ON DELETE CASCADE,
                               total_stages_completed INTEGER NOT NULL,
                               waypoints_passed_total INTEGER NOT NULL,
                               waypoints_missed_total INTEGER NOT NULL,
                               total_penalties_minutes INTEGER NOT NULL,
                               score DECIMAL(10,2) NOT NULL,
                               created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE auth_user (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          external_id VARCHAR(255),
                          email VARCHAR(255) NOT NULL UNIQUE,
                          username VARCHAR(255) UNIQUE,
                          password_hash VARCHAR(255),
                          role user_role NOT NULL,
                          profile_id UUID REFERENCES profile(id) ON DELETE SET NULL,
                          created_at TIMESTAMP DEFAULT NOW(),
                          updated_at TIMESTAMP DEFAULT NOW(),
                          status user_status DEFAULT 'ACTIVE'
);

CREATE INDEX idx_stage_rally_id ON stage (rally_id);
CREATE INDEX idx_waypoint_stage_id ON waypoint (stage_id);
CREATE INDEX idx_competitor_rally_id ON competitor (rally_id);
CREATE INDEX idx_competitor_stage_track_competitor_id ON competitor_stage_track (competitor_id);
CREATE INDEX idx_competitor_stage_track_stage_id ON competitor_stage_track (stage_id);
CREATE INDEX idx_waypoint_pass_competitor_stage_track_id ON waypoint_pass (competitor_stage_track_id);
CREATE INDEX idx_waypoint_pass_waypoint_id ON waypoint_pass (waypoint_id);
CREATE INDEX idx_profile_email ON profile (email);
CREATE INDEX idx_crew_member_competitor_id ON crew_member (competitor_id);
CREATE INDEX idx_crew_member_profile_id ON crew_member (profile_id);
