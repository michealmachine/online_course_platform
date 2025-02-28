"use client";

import { Button } from "@/components/ui/button";
import { ChevronLeft, ChevronRight } from "lucide-react";

interface PaginationProps {
  /**
   * 总记录数
   */
  total: number;
  /**
   * 当前页码
   */
  page: number;
  /**
   * 每页记录数
   */
  pageSize: number;
  /**
   * 显示的页码数量
   * @default 5
   */
  siblingCount?: number;
  onChange: (page: number) => void;
}

/**
 * 生成页码数组
 * @param currentPage 当前页码
 * @param totalPages 总页数
 * @param siblingCount 显示的页码数量
 * @returns 页码数组
 */
function generatePagination(
  currentPage: number,
  totalPages: number,
  siblingCount: number = 2
) {
  // 如果总页数小于需要显示的页码数，直接返回所有页码
  if (totalPages <= siblingCount * 2 + 3) {
    return Array.from({ length: totalPages }, (_, i) => i + 1);
  }

  // 计算左右两侧的页码范围
  const leftSiblingIndex = Math.max(currentPage - siblingCount, 1);
  const rightSiblingIndex = Math.min(currentPage + siblingCount, totalPages);

  // 是否显示左右两侧的省略号
  const shouldShowLeftDots = leftSiblingIndex > 2;
  const shouldShowRightDots = rightSiblingIndex < totalPages - 1;

  // 初始化页码数组
  const pages: (number | string)[] = [];

  // 添加第一页
  pages.push(1);

  // 添加左侧省略号
  if (shouldShowLeftDots) {
    pages.push('...');
  }

  // 添加当前页码周围的页码
  for (let i = leftSiblingIndex; i <= rightSiblingIndex; i++) {
    if (i !== 1 && i !== totalPages) {
      pages.push(i);
    }
  }

  // 添加右侧省略号
  if (shouldShowRightDots) {
    pages.push('...');
  }

  // 添加最后一页
  if (totalPages > 1) {
    pages.push(totalPages);
  }

  return pages;
}

/**
 * 分页组件
 * 提供页码导航功能，支持：
 * 1. 上一页/下一页导航
 * 2. 页码直接跳转
 * 3. 动态计算显示的页码范围
 * 4. 自动处理 URL 查询参数
 */
export function Pagination({ total, page, pageSize, siblingCount = 2, onChange }: PaginationProps) {
  const totalPages = Math.max(1, Math.ceil(total / pageSize)); // 至少有1页

  // 生成页码数组
  const pages = generatePagination(page, totalPages, siblingCount);

  return (
    <div className="flex items-center gap-2">
      {/* 上一页按钮 - 始终显示 */}
      <Button
        variant="outline"
        size="sm"
        onClick={() => onChange(Math.max(1, page - 1))}
        disabled={page === 1}
      >
        <ChevronLeft className="h-4 w-4" />
        上一页
      </Button>

      {/* 页码显示 */}
      <div className="flex items-center gap-1">
        <span className="text-sm">
          第 {page} 页 / 共 {totalPages} 页
        </span>
      </div>

      {/* 下一页按钮 - 始终显示 */}
      <Button
        variant="outline"
        size="sm"
        onClick={() => onChange(Math.min(totalPages, page + 1))}
        disabled={page >= totalPages}
      >
        下一页
        <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  );
} 