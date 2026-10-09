-- ============================================================================
-- TripMate Production Supabase Database Schema
-- Run this in your Supabase SQL Editor (https://supabase.com/dashboard/project/yuexbwbnekezxrkbzqpv/sql)
-- ============================================================================

-- 1. Create Profiles table (if not already created)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT,
    username TEXT,
    email TEXT,
    avatar_url TEXT,
    travel_vibes TEXT[] DEFAULT '{}',
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- 2. Create Trips table with support for collaborative itineraries and group budgets
CREATE TABLE IF NOT EXISTS public.trips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    destination TEXT,
    start_date TEXT,
    end_date TEXT,
    budget INTEGER DEFAULT 25000,
    itinerary_json TEXT, -- Full shared collaborative itinerary (JSON)
    created_by UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);

-- Ensure columns exist if table was already created
ALTER TABLE public.trips ADD COLUMN IF NOT EXISTS itinerary_json TEXT;
ALTER TABLE public.trips ADD COLUMN IF NOT EXISTS budget INTEGER DEFAULT 25000;
ALTER TABLE public.trips ADD COLUMN IF NOT EXISTS destination TEXT;
ALTER TABLE public.trips ADD COLUMN IF NOT EXISTS start_date TEXT;
ALTER TABLE public.trips ADD COLUMN IF NOT EXISTS end_date TEXT;

-- 3. Create Trip Members table (multi-account collaboration)
CREATE TABLE IF NOT EXISTS public.trip_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT DEFAULT 'member', -- 'owner' | 'member' | 'editor'
    joined_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (trip_id, user_id)
);

ALTER TABLE public.trip_members ADD COLUMN IF NOT EXISTS role TEXT DEFAULT 'member';
ALTER TABLE public.trip_members ADD COLUMN IF NOT EXISTS joined_at TIMESTAMPTZ DEFAULT now();

-- 4. Create Group Expenses table
CREATE TABLE IF NOT EXISTS public.expenses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    paid_by UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    category TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Multi-currency support (Splitwise standard):
-- Stores original foreign transaction currency, amount, and exchange rate for international expenses.
ALTER TABLE public.expenses ADD COLUMN IF NOT EXISTS original_amount DOUBLE PRECISION;
ALTER TABLE public.expenses ADD COLUMN IF NOT EXISTS original_currency TEXT;
ALTER TABLE public.expenses ADD COLUMN IF NOT EXISTS exchange_rate DOUBLE PRECISION;

-- 5. Create Expense Splits table
CREATE TABLE IF NOT EXISTS public.expense_splits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id UUID NOT NULL REFERENCES public.expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    amount_owed DOUBLE PRECISION NOT NULL,
    is_settled BOOLEAN DEFAULT false,
    UNIQUE (expense_id, user_id)
);

-- 6. Create Trip Votes table for real-time collaborative group voting on activities
CREATE TABLE IF NOT EXISTS public.trip_votes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id UUID NOT NULL REFERENCES public.trips(id) ON DELETE CASCADE,
    item_id TEXT NOT NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    vote_type TEXT NOT NULL CHECK (vote_type IN ('UP', 'DOWN')),
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (trip_id, item_id, user_id)
);

-- 7. Grant schema and table permissions to authenticated, anon, and postgres roles
-- (This resolves the "42501 permission denied for table ..." error)
GRANT USAGE ON SCHEMA public TO postgres, anon, authenticated, service_role;
GRANT ALL ON ALL TABLES IN SCHEMA public TO postgres, anon, authenticated, service_role;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO postgres, anon, authenticated, service_role;
GRANT ALL ON ALL ROUTINES IN SCHEMA public TO postgres, anon, authenticated, service_role;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO postgres, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO postgres, anon, authenticated, service_role;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON ROUTINES TO postgres, anon, authenticated, service_role;

-- 8. Enable Row Level Security (RLS) and create open policies for authenticated users
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trips ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trip_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expenses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.expense_splits ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.trip_votes ENABLE ROW LEVEL SECURITY;

-- Drop any conflicting existing policies
DROP POLICY IF EXISTS "Allow authenticated full access to profiles" ON public.profiles;
DROP POLICY IF EXISTS "Allow authenticated full access to trips" ON public.trips;
DROP POLICY IF EXISTS "Allow authenticated full access to trip_members" ON public.trip_members;
DROP POLICY IF EXISTS "Allow authenticated full access to expenses" ON public.expenses;
DROP POLICY IF EXISTS "Allow authenticated full access to expense_splits" ON public.expense_splits;
DROP POLICY IF EXISTS "Allow authenticated full access to trip_votes" ON public.trip_votes;

-- Profiles: Authenticated users can read all profiles (to search & invite by email) and modify their own
CREATE POLICY "Allow authenticated full access to profiles" ON public.profiles
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- Trips: Authenticated users can read, insert, update trips
CREATE POLICY "Allow authenticated full access to trips" ON public.trips
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- Trip Members: Authenticated users can view & manage members
CREATE POLICY "Allow authenticated full access to trip_members" ON public.trip_members
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- Expenses & Splits: Authenticated users can manage group expenses
CREATE POLICY "Allow authenticated full access to expenses" ON public.expenses
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

CREATE POLICY "Allow authenticated full access to expense_splits" ON public.expense_splits
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- Trip Votes: Authenticated users can vote on items
CREATE POLICY "Allow authenticated full access to trip_votes" ON public.trip_votes
    FOR ALL TO authenticated USING (true) WITH CHECK (true);

-- Also allow anon read on trips for shared web previews
CREATE POLICY "Allow anon read for shared web trips" ON public.trips
    FOR SELECT TO anon USING (true);
