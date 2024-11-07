package com.double2and9.content_service.controller;

import com.double2and9.content_service.dto.SaveTeachplanDTO;
import com.double2and9.content_service.dto.TeachplanDTO;
import com.double2and9.content_service.service.TeachplanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/teachplan")
public class TeachplanController {

    private final TeachplanService teachplanService;

    public TeachplanController(TeachplanService teachplanService) {
        this.teachplanService = teachplanService;
    }

    /**
     * 查询课程计划树形结构
     * @param courseId 课程id
     * @return 课程计划树形结构
     */
    @GetMapping("/tree/{courseId}")
    public List<TeachplanDTO> getTreeNodes(@PathVariable Long courseId) {
        return teachplanService.findTeachplanTree(courseId);
    }

    /**
     * 创建或修改课程计划
     */
    @PostMapping
    public void saveTeachplan(@RequestBody @Validated SaveTeachplanDTO teachplanDTO) {
        teachplanService.saveTeachplan(teachplanDTO);
    }

    /**
     * 删除课程计划
     */
    @DeleteMapping("/{teachplanId}")
    public void deleteTeachplan(@PathVariable Long teachplanId) {
        teachplanService.deleteTeachplan(teachplanId);
    }
} 