import { useQuery } from '@tanstack/react-query';
import { CourseService } from '@/services/course';

/**
 * 课程状态查询 Hook
 * 
 * 用于获取课程状态和审核状态列表
 * 包含缓存处理，默认 5 分钟更新一次
 * 
 * @returns {Object} 查询结果
 * @property {CourseStatusResponse} data - 状态数据
 * @property {boolean} isLoading - 加载状态
 * @property {Error} error - 错误信息
 * 
 * @example
 * function CourseStatusTag({ status }) {
 *   const { data, isLoading } = useCourseStatus();
 *   
 *   if (isLoading) return <Skeleton />;
 *   
 *   const statusInfo = data?.courseStatus.find(s => s.code === status);
 *   return <Tag>{statusInfo?.name || '未知状态'}</Tag>;
 * }
 */
export function useCourseStatus() {
  return useQuery({
    queryKey: ['courseStatus'],
    queryFn: () => CourseService.getCourseStatus(),
    staleTime: 1000 * 60 * 5, // 5分钟缓存
  });
} 