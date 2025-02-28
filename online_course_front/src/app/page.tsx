import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

export default function Home() {
  return (
    <div className="flex flex-col gap-8">
      <section className="space-y-6">
        <h1 className="text-3xl font-bold">欢迎来到在线课程平台</h1>
        <p className="text-muted-foreground">
          在这里，你可以找到最优质的课程资源，开启你的学习之旅。
        </p>
      </section>

      <section className="space-y-6">
        <h2 className="text-2xl font-semibold">推荐课程</h2>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardHeader>
                <CardTitle>示例课程 {i}</CardTitle>
                <CardDescription>这是一个示例课程描述</CardDescription>
              </CardHeader>
              <CardContent>
                <p>课程内容预览...</p>
              </CardContent>
            </Card>
          ))}
        </div>
      </section>

      <section className="space-y-6">
        <h2 className="text-2xl font-semibold">学习路径</h2>
        <div className="grid gap-4 md:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle>前端开发</CardTitle>
              <CardDescription>从零开始学习前端开发</CardDescription>
            </CardHeader>
            <CardContent>
              <p>包含 HTML, CSS, JavaScript, React 等课程</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle>后端开发</CardTitle>
              <CardDescription>掌握后端开发技能</CardDescription>
            </CardHeader>
            <CardContent>
              <p>包含 Node.js, Python, Java, 数据库等课程</p>
            </CardContent>
          </Card>
        </div>
      </section>
    </div>
  )
}
