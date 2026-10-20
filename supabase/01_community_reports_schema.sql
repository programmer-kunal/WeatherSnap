-- ==============================================================================
-- WeatherSnap: Community Reports Database Migration
-- Target: Supabase PostgreSQL (Public Schema)
-- ==============================================================================

-- 1. Create table for community weather reports
CREATE TABLE IF NOT EXISTS public.community_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    city_id INTEGER NOT NULL,
    city_name TEXT NOT NULL CHECK (length(trim(city_name)) > 0 AND length(city_name) <= 150),
    city_admin1 TEXT CHECK (city_admin1 IS NULL OR length(city_admin1) <= 150),
    city_country TEXT NOT NULL CHECK (length(trim(city_country)) > 0 AND length(city_country) <= 150),
    display_name TEXT NOT NULL CHECK (length(trim(display_name)) > 0 AND length(display_name) <= 300),
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude >= -90.0 AND latitude <= 90.0),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude >= -180.0 AND longitude <= 180.0),
    temperature DOUBLE PRECISION NOT NULL CHECK (temperature >= -100.0 AND temperature <= 70.0),
    condition TEXT NOT NULL CHECK (length(trim(condition)) > 0 AND length(condition) <= 100),
    humidity INTEGER NOT NULL CHECK (humidity >= 0 AND humidity <= 100),
    wind_speed DOUBLE PRECISION NOT NULL CHECK (wind_speed >= 0.0 AND wind_speed <= 200.0),
    pressure DOUBLE PRECISION NOT NULL CHECK (pressure >= 500.0 AND pressure <= 1200.0),
    notes TEXT NOT NULL DEFAULT '' CHECK (length(notes) <= 1000),
    image_path TEXT NOT NULL CHECK (
        length(trim(image_path)) > 0 
        AND length(image_path) <= 500
        AND array_length(string_to_array(image_path, '/'), 1) = 2
    ),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 2. Performance Indexes (Idempotent)
CREATE INDEX IF NOT EXISTS idx_community_reports_created_at 
    ON public.community_reports (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_community_reports_city_id_created 
    ON public.community_reports (city_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_community_reports_city_name_lower 
    ON public.community_reports (LOWER(city_name), created_at DESC);

CREATE INDEX IF NOT EXISTS idx_community_reports_user_id_created 
    ON public.community_reports (user_id, created_at DESC);

-- 3. Enable Row Level Security (RLS)
ALTER TABLE public.community_reports ENABLE ROW LEVEL SECURITY;

-- 4. Secure RLS Policies (Idempotent: DROP before CREATE)

-- Policy 1: Anyone (including unauthenticated & anonymous users) can view published reports
DROP POLICY IF EXISTS "Public community reports viewable by everyone" ON public.community_reports;
CREATE POLICY "Public community reports viewable by everyone"
    ON public.community_reports
    FOR SELECT
    USING (true);

-- Policy 2: Authenticated users can insert reports where user_id matches auth.uid()
-- AND the image_path strictly belongs to their folder: {auth.uid()}/{filename}
DROP POLICY IF EXISTS "Authenticated users can insert their own reports" ON public.community_reports;
CREATE POLICY "Authenticated users can insert their own reports"
    ON public.community_reports
    FOR INSERT
    TO authenticated
    WITH CHECK (
        auth.uid() IS NOT NULL 
        AND user_id = auth.uid()
        AND split_part(image_path, '/', 1) = auth.uid()::text
        AND array_length(string_to_array(image_path, '/'), 1) = 2
    );

-- Policy 3: Only the report author can delete their report
DROP POLICY IF EXISTS "Users can delete only their own reports" ON public.community_reports;
CREATE POLICY "Users can delete only their own reports"
    ON public.community_reports
    FOR DELETE
    TO authenticated
    USING (
        auth.uid() IS NOT NULL 
        AND user_id = auth.uid()
    );

-- Weather reports represent historical ground-truth observations: UPDATE is disallowed.
-- (No UPDATE policy is created)

-- 5. Least-Privilege Permissions: Revoke Defaults & Explicitly Grant Minimal Rights
REVOKE ALL ON TABLE public.community_reports FROM PUBLIC;
REVOKE ALL ON TABLE public.community_reports FROM anon, authenticated;

GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT SELECT ON TABLE public.community_reports TO anon, authenticated;
GRANT INSERT, DELETE ON TABLE public.community_reports TO authenticated;
