if redis.call('exists', KEYS[1]) == 1 then
  return tostring(redis.call('incr', KEYS[1]))
else
  return nil
end