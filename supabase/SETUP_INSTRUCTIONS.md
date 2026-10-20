# WeatherSnap: Supabase Backend Setup Instructions

This document provides step-by-step instructions to configure your Supabase backend for the WeatherSnap community reporting feature.

---

## 📋 Architectural Overview

1. **PostgreSQL Table (`community_reports`)**:
   - Stores meteorological observations, stable city identifiers (`city_id`), region/country metadata, field notes, coordinates, and photo object paths (`image_path`).
   - Weather reports are immutable ground-truth observations: `UPDATE` operations are disallowed.
2. **Database Row Level Security (RLS)**:
   - **Public Read (`SELECT`)**: Completely open to all clients (`anon` and `authenticated`) for immediate feed visibility.
   - **Enforced Author Ownership (`INSERT`)**: Authenticated anonymous sessions can only insert rows where `user_id = auth.uid()` **AND** `image_path` belongs to their personal directory (`split_part(image_path, '/', 1) = auth.uid()::text`). Identity spoofing and referencing other users' photos are strictly blocked.
   - **Author-Only Deletion (`DELETE`)**: Only the original author (`user_id = auth.uid()`) can delete their report.
3. **Storage Bucket (`report-photos`)**:
   - Marked as `public = true` with a 5 MB file size limit and strict image MIME validation (`image/jpeg`, `image/png`, `image/webp`).
   - Public GET requests (`/storage/v1/object/public/report-photos/{user_id}/{filename}.jpg`) are served directly by the Supabase Storage CDN without requiring broad database listing privileges.
4. **Storage Security Policies**:
   - **No Overwrite Policy**: `UPDATE` is disallowed. Uploads must use unique object paths with `upsert = false`.
   - **Folder Isolation**: Authenticated users can upload and delete files only within their own directory (`{authenticated_user_id}/{filename}`).
   - **Restricted Listing**: Calling `storage.from('report-photos').list()` is restricted to the authenticated author's folder, preventing third parties from scraping or enumerating the bucket directory.

---

## 🗺️ City Model Mapping (`City.kt` ↔ `community_reports`)

WeatherSnap uses Open-Meteo geocoding to retrieve structured city information. The database schema directly maps to the application's domain model ([`City.kt`](file:///D:/Apps/WeatherSnap/app/src/main/java/com/example/weathersnap/domain/model/City.kt)):

| `City.kt` Property | Database Column | SQL Type | Description & Purpose |
|---|---|---|---|
| `city.id` | `city_id` | `INTEGER NOT NULL` | Stable Open-Meteo numeric ID for exact same-city grouping. |
| `city.name` | `city_name` | `TEXT NOT NULL` | Primary city name (e.g., `"Ghaziabad"`, `"Jaipur"`). |
| `city.admin1` | `city_admin1` | `TEXT` | State or administrative division (e.g., `"Uttar Pradesh"`, `"Rajasthan"`). |
| `city.country` | `city_country` | `TEXT NOT NULL` | Country name (e.g., `"India"`, `"United Kingdom"`). |
| `city.displayName` | `display_name` | `TEXT NOT NULL` | Full formatted address string displayed in the UI header. |
| `city.latitude` | `latitude` | `DOUBLE PRECISION` | Geographic latitude coordinate for regional search. |
| `city.longitude` | `longitude` | `DOUBLE PRECISION` | Geographic longitude coordinate for regional search. |

---

## 🖼️ Storage Path Convention, Retries & Public URL Derivation

### 1. Storage API Path Convention
The bucket name is **`report-photos`**. When invoking the Supabase Storage API, the object path passed to the method must **NOT** include the bucket name:

```kotlin
// Format: {user_id}/{unique_filename}.jpg
val objectPath = "${userId}/${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"

// Example:
"d3b07384-d113-4a44-9321-c42661c94488/1715426189000_a1b2c3d4.jpg"
```

❌ **Incorrect**: `"report-photos/d3b07384.../image.jpg"` (Passing the bucket name in the path will fail RLS policy checks because folder `(storage.foldername(name))[1]` would evaluate to `"report-photos"` instead of `auth.uid()`).

### 2. Upload Design & Safe Retry Handling
- Uploads must specify **`upsert = false`**.
- Storage has no `UPDATE` policy. This guarantees that photographic evidence is write-once and cannot be silently replaced or tampered with.
- **Safe Retry Handling**: If an image upload or subsequent report insert encounters a network timeout or failure, the client must generate a **new unique filename** (new timestamp/UUID) upon retry. This avoids naming collisions and guarantees clean idempotent attempts.

### 3. Public URL Derivation
We store **`image_path`** in the `community_reports` table (e.g., `"d3b07384.../1715426189000_a1b2c3d4.jpg"`).

The public image URL is derived dynamically on the client:
```text
https://<project-ref>.supabase.co/storage/v1/object/public/report-photos/{image_path}
```

Or using the Supabase Kotlin SDK:
```kotlin
val publicUrl = supabase.storage.from("report-photos").publicUrl(report.imagePath)
```

**Why storing `image_path` is superior to storing full URLs**:
- Storing full URLs hardcodes project references, API endpoints, or domains into table rows.
- If the project ref changes, custom domains are configured, or CDN proxies are added, `image_path` remains 100% portable and requires zero database migrations.

---

## 🗑️ Safe Deletion Workflow

When an author deletes their report from the My Reports screen:

```
[User Confirms Delete]
         │
         ▼
[Step 1: Delete DB Record]
 DELETE FROM community_reports WHERE id = :id AND user_id = auth.uid()
         │
    ┌────┴────┐
    │ Success │
    ▼         ▼
[Step 2: Delete Storage File]
 storage.from("report-photos").delete(listOf(image_path))
    │
    ▼
[Update UI State Immediately]
```

### Failure Analysis & Orphan Handling:
1. **If Step 1 (DB Delete) Fails**:
   - The operation aborts immediately. The storage file is untouched.
   - The report remains visible, and the user is shown an error message.
2. **If Step 1 Succeeds but Step 2 (Storage Delete) Fails** (e.g., network disconnect):
   - The report is already removed from the database and vanishes from the public feed immediately.
   - The storage object remains harmlessly in the author's personal folder (`{user_id}/`). It cannot be accessed through the feed because no database record links to it.
   - The client can safely retry deleting the path because the Storage policy prevents deleting any other user's files.
3. **Cross-User Attack Prevention**:
   - Even if a malicious client sends another user's `id` or `image_path`, both the database RLS (`user_id = auth.uid()`) and the Storage RLS (`(storage.foldername(name))[1] = auth.uid()::text`) reject the deletion.

---

## 🛠️ Migration & Dashboard Execution Guide

### Strategy A: Fresh Supabase Project (Recommended)
If this is a fresh project or you do not have production reports to preserve:

1. In the [Supabase Dashboard](https://supabase.com/dashboard), navigate to **Authentication** → **Providers** → **Anonymous Sign-Ins**. Ensure the toggle is **Enabled** and click **Save**.
2. Go to **SQL Editor** → **+ New Query**.
3. Paste the entire content of [`01_community_reports_schema.sql`](./01_community_reports_schema.sql) and click **Run**.
4. Open a **New Query**, paste the entire content of [`02_storage_setup.sql`](./02_storage_setup.sql), and click **Run**.
5. In **Project Settings** → **API**, copy your **Project URL** and **`anon` `public`** key (never copy the `service_role` key).

---

### Strategy B: Explicit Migration (If an Early Prototype Table Already Exists)
`CREATE TABLE IF NOT EXISTS` will **not** alter existing columns, remove obsolete columns, or retroactively apply new `CHECK` constraints if an older schema was previously run.

If an early prototype table exists and you want to cleanly upgrade to the final schema without losing test data, execute this explicit migration block in the SQL Editor before running the main schema:

```sql
-- Explicit Upgrade Migration (Run ONLY if an older prototype table exists)
DO $$
BEGIN
    -- 1. Rename image_url to image_path if present
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'community_reports' AND column_name = 'image_url') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'community_reports' AND column_name = 'image_path') THEN
        ALTER TABLE public.community_reports RENAME COLUMN image_url TO image_path;
    END IF;

    -- 2. Add structured geographic columns if missing
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'community_reports' AND column_name = 'city_id') THEN
        ALTER TABLE public.community_reports ADD COLUMN city_id INTEGER NOT NULL DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'community_reports' AND column_name = 'city_admin1') THEN
        ALTER TABLE public.community_reports ADD COLUMN city_admin1 TEXT;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'community_reports' AND column_name = 'city_country') THEN
        ALTER TABLE public.community_reports ADD COLUMN city_country TEXT NOT NULL DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'community_reports' AND column_name = 'display_name') THEN
        ALTER TABLE public.community_reports ADD COLUMN display_name TEXT NOT NULL DEFAULT '';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'community_reports' AND column_name = 'latitude') THEN
        ALTER TABLE public.community_reports ADD COLUMN latitude DOUBLE PRECISION NOT NULL DEFAULT 0.0;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'public' AND table_name = 'community_reports' AND column_name = 'longitude') THEN
        ALTER TABLE public.community_reports ADD COLUMN longitude DOUBLE PRECISION NOT NULL DEFAULT 0.0;
    END IF;
END $$;
```
*(If the prototype table has no valuable data, simply run `DROP TABLE IF EXISTS public.community_reports CASCADE;` and then run `01_community_reports_schema.sql` cleanly).*
