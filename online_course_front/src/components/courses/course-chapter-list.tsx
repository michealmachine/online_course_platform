"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { contentRequest } from "@/services/index";
import { API_URLS } from "@/config/api.config";
import { ChapterForm } from "./chapter-form";
import { SectionForm } from "./section-form";
import { CourseService, type TeachplanDTO } from "@/services/course";

interface TeachplanNode {
  id: number;
  name: string;
  courseId: number;
  parentId: number;
  level: number;
  orderBy: number;
  children?: TeachplanNode[];
}

interface CourseChapterListProps {
  courseId: number;
  organizationId: number;
}

export function CourseChapterList({ courseId, organizationId }: CourseChapterListProps) {
  const [chapters, setChapters] = useState<TeachplanNode[]>([]);
  const [loading, setLoading] = useState(true);
  const { toast } = useToast();

  // 对话框状态
  const [chapterDialogOpen, setChapterDialogOpen] = useState(false);
  const [sectionDialogOpen, setSectionDialogOpen] = useState(false);
  const [selectedChapter, setSelectedChapter] = useState<TeachplanNode | null>(null);
  const [selectedSection, setSelectedSection] = useState<TeachplanNode | null>(null);

  // 删除确认对话框状态
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [itemToDelete, setItemToDelete] = useState<{
    id: number;
    name: string;
    isChapter: boolean;
  } | null>(null);

  const [hasOrderChanges, setHasOrderChanges] = useState(false);

  useEffect(() => {
    loadTeachplan();
  }, [courseId]);

  const loadTeachplan = async () => {
    try {
      setLoading(true);
      const data = await CourseService.getTeachplanTree(courseId);
      setChapters(data);
    } catch (error) {
      toast({
        title: "加载失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  // 处理章节移动
  const handleMove = async (id: number, direction: 'up' | 'down') => {
    try {
      if (direction === 'up') {
        await CourseService.moveUpTeachplan(id);
      } else {
        await CourseService.moveDownTeachplan(id);
      }
      setHasOrderChanges(true); // 标记有未保存的排序变更
      loadTeachplan(); // 加载临时排序结果
    } catch (error) {
      // 处理业务异常
      const message = error instanceof Error ? error.message : "未知错误";
      if (message.includes("已经是第一个") || message.includes("已经是最后一个")) {
        toast({
          title: "无法移动",
          description: message,
        });
      } else {
        toast({
          title: "移动失败",
          description: message,
          variant: "destructive",
        });
      }
    }
  };

  // 保存排序变更
  const handleSaveOrder = async () => {
    try {
      await CourseService.saveTeachplanOrder();
      setHasOrderChanges(false);
      toast({
        title: "保存成功",
        description: "排序变更已永久保存",
      });
      loadTeachplan(); // 重新加载最终结果
    } catch (error) {
      toast({
        title: "保存失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  // 丢弃排序变更
  const handleDiscardOrder = async () => {
    try {
      await CourseService.discardTeachplanOrder();
      setHasOrderChanges(false);
      toast({
        title: "已重置",
        description: "排序变更已丢弃",
      });
      loadTeachplan(); // 重新加载原始顺序
    } catch (error) {
      toast({
        title: "重置失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    }
  };

  // 处理添加/编辑章节
  const handleChapter = (chapter?: TeachplanNode) => {
    setSelectedChapter(chapter || null);
    setChapterDialogOpen(true);
  };

  // 处理添加/编辑小节
  const handleSection = (chapter: TeachplanNode, section?: TeachplanNode) => {
    setSelectedChapter(chapter);
    setSelectedSection(section || null);
    setSectionDialogOpen(true);
  };

  // 处理删除
  const handleDelete = async (id: number, name: string, isChapter: boolean) => {
    setItemToDelete({ id, name, isChapter });
    setDeleteDialogOpen(true);
  };

  // 确认删除
  const confirmDelete = async () => {
    if (!itemToDelete) return;

    try {
      await CourseService.deleteTeachplan(itemToDelete.id);
      
      toast({
        title: "删除成功",
        description: `${itemToDelete.isChapter ? "章节" : "小节"} "${itemToDelete.name}" 已删除`,
      });
      
      loadTeachplan(); // 重新加载数据
    } catch (error) {
      toast({
        title: "删除失败",
        description: error instanceof Error ? error.message : "未知错误",
        variant: "destructive",
      });
    } finally {
      setDeleteDialogOpen(false);
      setItemToDelete(null);
    }
  };

  // 添加更新状态的方法
  const updateChapters = (newNode: TeachplanNode) => {
    if (newNode.level === 1) {
      // 添加新章节
      setChapters(prev => [...prev, { ...newNode, children: [] }]);
    } else {
      // 添加新小节
      setChapters(prev => prev.map(chapter => {
        if (chapter.id === newNode.parentId) {
          return {
            ...chapter,
            children: [...(chapter.children || []), newNode],
          };
        }
        return chapter;
      }));
    }
  };

  // 渲染章节
  const renderChapter = (chapter: TeachplanNode, chapterIndex: number) => (
    <Card key={chapter.id} className="mb-4">
      <CardHeader>
        <CardTitle className="flex justify-between items-center">
          <span className="flex items-center gap-2">
            <span className="text-sm px-2 py-1 bg-primary/10 rounded-md">
              第{chapterIndex + 1}章
            </span>
            <span>{chapter.name}</span>
          </span>
          <div className="space-x-2">
            <Button 
              variant="outline" 
              size="sm"
              onClick={() => handleSection(chapter)}
            >
              添加小节
            </Button>
            <Button 
              variant="outline" 
              size="sm"
              onClick={() => handleChapter(chapter)}
            >
              编辑
            </Button>
            <Button 
              variant="outline" 
              size="sm"
              onClick={() => handleMove(chapter.id, 'up')}
              disabled={chapterIndex === 0}
            >
              上移
            </Button>
            <Button 
              variant="outline" 
              size="sm"
              onClick={() => handleMove(chapter.id, 'down')}
              disabled={chapterIndex === chapters.length - 1}
            >
              下移
            </Button>
            <Button 
              variant="destructive" 
              size="sm"
              onClick={() => handleDelete(chapter.id, chapter.name, true)}
            >
              删除
            </Button>
          </div>
        </CardTitle>
      </CardHeader>
      <CardContent>
        {chapter.children?.map((section, sectionIndex) => (
          <div
            key={section.id}
            className="flex justify-between items-center p-2 hover:bg-gray-100 rounded"
          >
            <span className="flex items-center gap-2">
              <span className="text-sm text-muted-foreground">
                {chapterIndex + 1}.{sectionIndex + 1}
              </span>
              <span>{section.name}</span>
            </span>
            <div className="space-x-2">
              <Button 
                variant="ghost" 
                size="sm"
                onClick={() => handleSection(chapter, section)}
              >
                编辑
              </Button>
              <Button 
                variant="ghost" 
                size="sm"
                onClick={() => handleMove(section.id, 'up')}
                disabled={sectionIndex === 0}
              >
                上移
              </Button>
              <Button 
                variant="ghost" 
                size="sm"
                onClick={() => handleMove(section.id, 'down')}
                disabled={sectionIndex === (chapter.children?.length || 0) - 1}
              >
                下移
              </Button>
              <Button 
                variant="ghost" 
                size="sm"
                className="text-destructive hover:text-destructive"
                onClick={() => handleDelete(section.id, section.name, false)}
              >
                删除
              </Button>
            </div>
          </div>
        ))}
        {/* 如果没有小节，显示提示信息 */}
        {(!chapter.children || chapter.children.length === 0) && (
          <div className="text-center text-muted-foreground py-4">
            暂无小节内容，请添加小节
          </div>
        )}
      </CardContent>
    </Card>
  );

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <div className="space-y-1">
          <h2 className="text-2xl font-bold">课程计划</h2>
          <p className="text-sm text-muted-foreground">
            管理课程的章节结构，可以添加、编辑、排序章节和小节
            {hasOrderChanges && (
              <span className="text-yellow-500 ml-2">
                (有未保存的排序变更)
              </span>
            )}
          </p>
        </div>
        <div className="space-x-2">
          {hasOrderChanges && (
            <>
              <Button variant="outline" onClick={handleDiscardOrder}>
                重置排序
              </Button>
              <Button onClick={handleSaveOrder}>
                保存排序
              </Button>
            </>
          )}
          <Button onClick={() => handleChapter()}>添加章节</Button>
        </div>
      </div>
      
      {loading ? (
        <div className="text-center py-8 text-muted-foreground">加载中...</div>
      ) : (
        <div className="space-y-4">
          {chapters.length === 0 ? (
            <Card>
              <CardContent className="text-center py-8">
                <p className="text-muted-foreground">暂无课程计划，请添加章节</p>
                <Button 
                  className="mt-4" 
                  variant="outline"
                  onClick={() => handleChapter()}
                >
                  添加第一章
                </Button>
              </CardContent>
            </Card>
          ) : (
            chapters.map((chapter, index) => renderChapter(chapter, index))
          )}
        </div>
      )}

      {/* 章节表单对话框 */}
      <ChapterForm
        courseId={courseId}
        open={chapterDialogOpen}
        onOpenChange={setChapterDialogOpen}
        onSuccess={updateChapters}
        initialData={selectedChapter ? {
          id: selectedChapter.id,
          name: selectedChapter.name,
          orderBy: selectedChapter.orderBy,
        } : undefined}
      />

      {/* 小节表单对话框 */}
      {selectedChapter && (
        <SectionForm
          courseId={courseId}
          chapterId={selectedChapter.id}
          open={sectionDialogOpen}
          onOpenChange={setSectionDialogOpen}
          onSuccess={updateChapters}
          initialData={selectedSection ? {
            id: selectedSection.id,
            name: selectedSection.name,
            orderBy: selectedSection.orderBy,
          } : undefined}
        />
      )}

      {/* 删除确认对话框 */}
      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除</AlertDialogTitle>
            <AlertDialogDescription>
              {itemToDelete?.isChapter 
                ? `确定要删除章节 "${itemToDelete.name}" 吗？删除章节将同时删除其下所有小节。`
                : `确定要删除小节 "${itemToDelete?.name}" 吗？此操作无法撤销。`
              }
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction onClick={confirmDelete}>删除</AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
} 