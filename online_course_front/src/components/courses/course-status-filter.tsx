"use client";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

interface Props {
  value: string;
  onChange: (value: string) => void;
  type?: 'status' | 'audit';
}

// 课程状态选项
const STATUS_OPTIONS = [
  { value: 'all', label: '全部状态' },
  { value: '202001', label: '未发布' },
  { value: '202002', label: '已发布' },
  { value: '202003', label: '已下线' }
];

// 审核状态选项
const AUDIT_STATUS_OPTIONS = [
  { value: 'all', label: '全部审核状态' },
  { value: '202301', label: '待审核' },
  { value: '202302', label: '审核不通过' },
  { value: '202303', label: '审核通过' }
];

/**
 * 课程状态筛选组件
 * 
 * @example
 * <CourseStatusFilter
 *   value={status}
 *   onChange={setStatus}
 *   type="status"
 * />
 */
export function CourseStatusFilter({ value, onChange, type = 'status' }: Props) {
  const options = type === 'status' ? STATUS_OPTIONS : AUDIT_STATUS_OPTIONS;
  
  // 处理空值情况
  const currentValue = value || 'all';

  return (
    <Select 
      value={currentValue} 
      onValueChange={(val) => onChange(val === 'all' ? '' : val)}
    >
      <SelectTrigger className="w-[180px]">
        <SelectValue placeholder={`选择${type === 'status' ? '课程' : '审核'}状态`} />
      </SelectTrigger>
      <SelectContent>
        {options.map(option => (
          <SelectItem key={option.value} value={option.value}>
            {option.label}
          </SelectItem>
        ))}
      </SelectContent>
    </Select>
  );
} 