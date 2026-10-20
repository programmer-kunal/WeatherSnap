# WeatherSnap: Supabase Backend Verification Checklist

Follow this guide to thoroughly test and verify your database schema, Storage configuration, and Row Level Security (RLS) policies.

---

## 🔍 Verification Matrix

| Category | Test Case | Target / Policy | Expected Result | Pass / Fail |
|---|---|---|---|---|
| **Auth** | Anonymous sign-in enabled | Supabase Auth Provider | Token returned on anonymous signup | [ ] |
| **Schema** | `community_reports` table & columns | `city_id`, `city_name`, `image_path`, etc. | Columns & constraints created | [ ] |
| **Indexes** | Performance indexes | `created_at`, `city_id`, `LOWER(city_name)` | Indexes active in database | [ ] |
| **Storage Bucket** | `report-photos` created | Bucket Configuration | Bucket has `public = true` & 5 MB limit | [ ] |
| **Public Feed** | Fetching reports with no token | `public_read` policy | Returns published rows (HTTP 200) | [ ] |
| **Public Photo** | Downloading image from public URL | `/object/public/report-photos/...` | Image renders without auth header | [ ] |
| **Restricted Listing** | Calling Storage `.list()` without token | Storage `SELECT` policy | Denied / 0 rows from other folders | [ ] |
| **Valid Publish** | User A inserts row with matching `image_path` | RLS `INSERT` policy | Success (HTTP 201 Created) | [ ] |
| **Impersonation** | User B inserts row with `user_id = user_id_A` | RLS `INSERT` policy | Blocked (HTTP 403 / 42501 check violation) | [ ] |
| **Cross-Folder Image** | User B inserts row referencing User A's photo | RLS `INSERT` policy | Blocked (HTTP 403 / 42501 check violation) | [ ] |
| **Owner Delete** | User A deletes own report | RLS `DELETE` policy | Success (HTTP 204, row deleted) | [ ] |
| **Cross-User Delete** | User B attempts to delete User A's report | RLS `DELETE` policy | No rows affected / 0 deleted | [ ] |
| **Storage Upload** | User A uploads to `<USER_ID_A>/photo.jpg` | Storage `INSERT` policy | Success (HTTP 200) | [ ] |
| **Spoofed Upload** | User B uploads to `<USER_ID_A>/photo.jpg` | Storage `INSERT` policy | Rejected (HTTP 403 Forbidden) | [ ] |

---

## 🧪 API-Level Verification Tests (cURL / HTTP)

> **Note on Testing**: Executing `SET ROLE anon;` or `SET ROLE authenticated;` directly inside the SQL Editor does not populate `auth.uid()`, because `auth.uid()` is extracted by PostgREST from incoming JWT tokens. To test authenticated user policies accurately, use these standard HTTP calls.

Replace `<PROJECT_REF>` and `<ANON_KEY>` with your project credentials.

---

### Test 1: Public Community Feed Reads (Unauthenticated)
Verify that anyone can retrieve the community feed without authentication:

```bash
curl -i -X GET 'https://<PROJECT_REF>.supabase.co/rest/v1/community_reports?select=*&order=created_at.desc&limit=10' \
  -H "apikey: <ANON_KEY>"
```
- **Expected Status**: `HTTP/1.1 200 OK`
- **Output**: JSON array of published community reports.

---

### Test 2: Create Two Anonymous User Sessions
Create two isolated anonymous users (User A and User B) to verify ownership boundaries:

```bash
# Create User A:
curl -s -X POST 'https://<PROJECT_REF>.supabase.co/auth/v1/signup' \
  -H "apikey: <ANON_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"data": {}}' > user_a.json

# Create User B:
curl -s -X POST 'https://<PROJECT_REF>.supabase.co/auth/v1/signup' \
  -H "apikey: <ANON_KEY>" \
  -H "Content-Type: application/json" \
  -d '{"data": {}}' > user_b.json
```
Extract `access_token` and `user.id` from both responses.

---

### Test 3: Storage Upload by User A (upsert = false)
User A uploads a photograph into their dedicated folder:

```bash
curl -i -X POST 'https://<PROJECT_REF>.supabase.co/storage/v1/object/report-photos/<USER_ID_A>/test_photo.jpg' \
  -H "apikey: <ANON_KEY>" \
  -H "Authorization: Bearer <ACCESS_TOKEN_A>" \
  -H "Content-Type: image/jpeg" \
  --data-binary "sample_image_data"
```
- **Expected Status**: `HTTP/1.1 200 OK`

---

### Test 4: Legitimate Publish by User A
Publish a report where `user_id` matches User A's `auth.uid()` **AND** `image_path` begins with `<USER_ID_A>/`:

```bash
curl -i -X POST 'https://<PROJECT_REF>.supabase.co/rest/v1/community_reports' \
  -H "apikey: <ANON_KEY>" \
  -H "Authorization: Bearer <ACCESS_TOKEN_A>" \
  -H "Content-Type: application/json" \
  -H "Prefer: return=representation" \
  -d '{
    "user_id": "<USER_ID_A>",
    "city_id": 1271157,
    "city_name": "Ghaziabad",
    "city_admin1": "Uttar Pradesh",
    "city_country": "India",
    "display_name": "Ghaziabad, Uttar Pradesh, India",
    "latitude": 28.6692,
    "longitude": 77.4538,
    "temperature": 27.5,
    "condition": "Clear sky",
    "humidity": 55,
    "wind_speed": 6.2,
    "pressure": 990.0,
    "notes": "Verified clear sky ground truth test.",
    "image_path": "<USER_ID_A>/test_photo.jpg"
  }'
```
- **Expected Status**: `HTTP/1.1 201 Created`
- Note the generated report `id`.

---

### Test 5: Cross-Folder Photo Reference Attack
User B attempts to create a report referencing User A's photo in `image_path`:

```bash
curl -i -X POST 'https://<PROJECT_REF>.supabase.co/rest/v1/community_reports' \
  -H "apikey: <ANON_KEY>" \
  -H "Authorization: Bearer <ACCESS_TOKEN_B>" \
  -H "Content-Type: application/json" \
  -d '{
    "user_id": "<USER_ID_B>",
    "city_id": 1271157,
    "city_name": "Ghaziabad",
    "city_admin1": "Uttar Pradesh",
    "city_country": "India",
    "display_name": "Ghaziabad, Uttar Pradesh, India",
    "latitude": 28.6692,
    "longitude": 77.4538,
    "temperature": 27.5,
    "condition": "Clear sky",
    "humidity": 55,
    "wind_speed": 6.2,
    "pressure": 990.0,
    "notes": "Trying to steal User A photo reference",
    "image_path": "<USER_ID_A>/test_photo.jpg"
  }'
```
- **Expected Status**: `HTTP/1.1 403 Forbidden` (RLS policy check failure: `split_part(image_path, '/', 1) != auth.uid()`).

---

### Test 6: Cross-User Report Deletion Prevention
User B attempts to delete User A's published report:

```bash
curl -i -X DELETE 'https://<PROJECT_REF>.supabase.co/rest/v1/community_reports?id=eq.<REPORT_ID_A>' \
  -H "apikey: <ANON_KEY>" \
  -H "Authorization: Bearer <ACCESS_TOKEN_B>" \
  -H "Prefer: return=representation"
```
- **Expected Status**: `HTTP/1.1 200 OK` or `204 No Content` with an **empty array `[]`** returned.
- Re-query the report as in Test 1 to confirm User A's report was **not** deleted.

---

### Test 7: Public Image Fetch (No Auth Required)
Verify that public photo URLs work without any bearer token:

```bash
curl -i -X GET 'https://<PROJECT_REF>.supabase.co/storage/v1/object/public/report-photos/<USER_ID_A>/test_photo.jpg'
```
- **Expected Status**: `HTTP/1.1 200 OK`
- Returns the image binary data directly.

---

### Test 8: Legitimate Owner Deletion by User A
User A deletes their own report:

```bash
curl -i -X DELETE 'https://<PROJECT_REF>.supabase.co/rest/v1/community_reports?id=eq.<REPORT_ID_A>' \
  -H "apikey: <ANON_KEY>" \
  -H "Authorization: Bearer <ACCESS_TOKEN_A>" \
  -H "Prefer: return=representation"
```
- **Expected Status**: `HTTP/1.1 200 OK` returning the deleted record representation.
- Re-query confirms the record is removed.
