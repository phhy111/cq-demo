package edu.cqie.cqdemo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.cqie.cqdemo.entity.Guides;
import edu.cqie.cqdemo.mapper.GuidesMapper;
import edu.cqie.cqdemo.service.GuidesService;
import org.springframework.stereotype.Service;

@Service
public class GuidesServiceImpl extends ServiceImpl<GuidesMapper, Guides> implements GuidesService {
}
