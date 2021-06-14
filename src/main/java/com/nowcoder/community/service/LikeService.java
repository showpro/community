package com.nowcoder.community.service;

import com.nowcoder.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 点赞
     *
     * @param userId
     * @param entityType
     * @param entityId
     * @param entityUserId 就是被点赞的user的Id
     */
    public void like(int userId, int entityType, int entityId, int entityUserId) {
/*        String entityLikeKey = RedisUtil.getEntityLikeKey(entityType,entityId);
        //第一次点是点赞，第二次点是取消赞
        //先判断是否点过赞  value是set集合存的是userId
        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
        if(isMember){
            //说明点过赞，这次是取消赞
            redisTemplate.opsForSet().remove(entityLikeKey,userId);
        }else{
            //说明是第一次点赞
            redisTemplate.opsForSet().add(entityLikeKey,userId);
        }*/
        //编程式事务
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                //判断当前用户有没有点赞，这一步应该在事务开启前执行，因为在事务中的查询不会立即得到结果
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);

                //事务开启
                operations.multi();

                if (isMember) {
                    //说明点过赞，这次是取消赞
                    operations.opsForSet().remove(entityLikeKey, userId);
                    //被点赞的用户点赞数量减一
                    operations.opsForValue().decrement(userLikeKey);
                } else {
                    //说明是第一次点赞
                    operations.opsForSet().add(entityLikeKey, userId);
                    //被点赞的用户点赞数量加一
                    operations.opsForValue().increment(userLikeKey);
                }

                return operations.exec();
            }
        });
    }

    // 查询某实体点赞的数量
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }

    // 查询某个用户获得的赞
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count == null ? 0 : count.intValue();
    }

}
