package edu.cqie.cqdemo.mapper;

import edu.cqie.cqdemo.entity.Categories;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CategoriesMapper {
    List<Categories> selectcategories();
}
