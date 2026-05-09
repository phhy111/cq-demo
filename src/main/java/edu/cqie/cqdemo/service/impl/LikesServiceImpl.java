package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.Likes;
import edu.cqie.cqdemo.mapper.LikesMapper;
import edu.cqie.cqdemo.service.LikesService;
import org.springframework.stereotype.Service;

@Service
public class LikesServiceImpl extends ServiceImpl<LikesMapper, Likes> implements LikesService {
}
