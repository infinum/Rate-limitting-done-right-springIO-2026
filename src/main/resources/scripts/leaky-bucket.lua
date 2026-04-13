-- Leaky Bucket Rate Limiter
-- KEYS[1] = bucket key (e.g. "leaky:{tenantId}")
-- ARGV[1] = capacity (max drops in bucket)
-- ARGV[2] = leak rate (drops per second)
-- ARGV[3] = current time in milliseconds

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local leakRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local bucket = redis.call('HMGET', key, 'level', 'lastLeak')
local level = tonumber(bucket[1]) or 0
local lastLeak = tonumber(bucket[2]) or now

-- Drain: calculate how many drops leaked since last request
local elapsed = (now - lastLeak) / 1000.0
local leaked = math.floor(elapsed * leakRate)
level = math.max(0, level - leaked)

-- Update lastLeak only by the amount actually leaked (avoid drift)
if leaked > 0 then
    lastLeak = lastLeak + (leaked / leakRate) * 1000
end

-- Try to add a drop
if level < capacity then
    level = level + 1
    redis.call('HSET', key, 'level', level, 'lastLeak', lastLeak)
    redis.call('PEXPIRE', key, math.ceil(capacity / leakRate) * 1000 + 1000)
    return {1, capacity - level}  -- allowed, remaining capacity
else
    redis.call('HSET', key, 'level', level, 'lastLeak', lastLeak)
    redis.call('PEXPIRE', key, math.ceil(capacity / leakRate) * 1000 + 1000)
    -- Calculate retry-after: time until 1 drop leaks
    local retryMs = math.ceil((1 / leakRate) * 1000)
    return {0, retryMs}  -- rejected, retry after ms
end
