-- Custom Token Bucket Rate Limiter
-- KEYS[1] = bucket key (e.g. ratelimit:192.168.1.1)
-- ARGV[1] = max capacity
-- ARGV[2] = refill rate (tokens per second)
-- ARGV[3] = current time in seconds
-- ARGV[4] = requested tokens

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- Lấy state hiện tại của bucket
local bucket = redis.call('HGETALL', key)
local tokens = capacity
local last_refill = now

if #bucket > 0 then
    for i=1, #bucket, 2 do
        if bucket[i] == 'tokens' then
            tokens = tonumber(bucket[i+1])
        elseif bucket[i] == 'last_refill' then
            last_refill = tonumber(bucket[i+1])
        end
    end
end

-- Tính toán số token mới được bơm thêm dựa trên thời gian trôi qua
local time_passed = math.max(0, now - last_refill)
local new_tokens = math.min(capacity, tokens + (time_passed * refill_rate))

local allowed = 0
-- Kiểm tra xem có đủ token để phục vụ request không
if new_tokens >= requested then
    new_tokens = new_tokens - requested
    allowed = 1
end

-- Lưu state mới
redis.call('HSET', key, 'tokens', new_tokens, 'last_refill', now)

-- Set TTL để giải phóng RAM nếu user không gọi nữa
local ttl = math.ceil(capacity / refill_rate) * 2
redis.call('EXPIRE', key, ttl)

return allowed
