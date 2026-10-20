-- ==============================================================================
-- WeatherSnap: Supabase Storage Configuration & Policies (Idempotent)
-- Bucket Name: report-photos
-- Object Path Convention: {user_id}/{unique_filename}.jpg
-- ==============================================================================

-- 1. Create or update the 'report-photos' storage bucket
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'report-photos',
    'report-photos',
    true,                   -- Public bucket: images are directly downloadable via /object/public/
    5242880,                -- 5 MB maximum file size limit
    ARRAY['image/jpeg', 'image/png', 'image/webp']::text[]
)
ON CONFLICT (id) DO UPDATE SET
    public = true,
    file_size_limit = 5242880,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp']::text[];

-- 2. Storage Row Level Security Policies on storage.objects

-- Ensure legacy / obsolete policies are dropped cleanly
DROP POLICY IF EXISTS "Public access to report photos" ON storage.objects;
DROP POLICY IF EXISTS "Users can update their own report photos" ON storage.objects;

-- Policy 1: Restrict metadata/listing SELECT to the owner's own folder
-- NOTE: Public downloads (/storage/v1/object/public/report-photos/{user_id}/{filename}.jpg)
-- are served directly by the Supabase Storage gateway because public=true on the bucket.
-- This SELECT policy permits authors to list or query their own files while
-- blocking unauthenticated or third-party enumeration of the entire bucket.
DROP POLICY IF EXISTS "Users can view own report photos metadata" ON storage.objects;
CREATE POLICY "Users can view own report photos metadata"
    ON storage.objects
    FOR SELECT
    TO authenticated
    USING (
        bucket_id = 'report-photos'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- Policy 2: Authenticated users can upload photos only to their own directory: {user_id}/{filename}
-- Uploads must use unique object paths with upsert=false (no overwriting allowed)
DROP POLICY IF EXISTS "Users can upload report photos to their own directory" ON storage.objects;
CREATE POLICY "Users can upload report photos to their own directory"
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'report-photos'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    );

-- Policy 3: Users can delete only their own uploaded photos
DROP POLICY IF EXISTS "Users can delete only their own report photos" ON storage.objects;
CREATE POLICY "Users can delete only their own report photos"
    ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'report-photos'
        AND auth.uid() IS NOT NULL
        AND (storage.foldername(name))[1] = auth.uid()::text
    );
