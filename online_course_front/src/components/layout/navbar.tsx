import Link from 'next/link'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'

export function Navbar() {
  return (
    <header className="fixed top-0 left-0 right-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <nav className="container flex h-14 items-center">
        <div className="mr-4 flex">
          <Link href="/" className="mr-6 flex items-center space-x-2">
            <span className="font-bold">在线课程平台</span>
          </Link>
        </div>

        <div className="flex flex-1 items-center justify-between space-x-2 md:justify-end">
          <div className="w-full flex-1 md:w-auto md:flex-none">
            {/* 这里可以添加搜索框 */}
          </div>
          <nav className="flex items-center space-x-2">
            <Button variant="ghost" size="sm">
              课程
            </Button>
            <Button variant="ghost" size="sm">
              学习路径
            </Button>
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" className="relative size-8 rounded-full">
                  <Avatar className="size-8">
                    <AvatarImage src="" alt="用户头像" />
                    <AvatarFallback>用户</AvatarFallback>
                  </Avatar>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem>
                  个人中心
                </DropdownMenuItem>
                <DropdownMenuItem>
                  我的课程
                </DropdownMenuItem>
                <DropdownMenuItem>
                  设置
                </DropdownMenuItem>
                <DropdownMenuItem>
                  退出
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </nav>
        </div>
      </nav>
    </header>
  )
} 