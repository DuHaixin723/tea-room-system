// 前端源码文件：承载当前模块的页面或通用逻辑。

declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}

declare module 'nprogress'
declare module 'sockjs-client'
