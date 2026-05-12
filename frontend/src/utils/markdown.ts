// 工具函数：封装格式化、存储和状态转换等通用能力。

function escapeHtml(input: string) {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function formatInline(text: string) {
  return text
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/\[([^\]]+)]\((https?:\/\/[^)]+)\)/g, '<a href="$2" target="_blank" rel="noreferrer">$1</a>')
}

export function renderMarkdown(markdown: string) {
  const normalized = escapeHtml(markdown).replace(/\r\n/g, '\n')
  const lines = normalized.split('\n')
  const html: string[] = []
  let inCodeBlock = false
  let codeBuffer: string[] = []
  let listBuffer: string[] = []

  const flushList = () => {
    if (!listBuffer.length) return
    html.push(`<ul>${listBuffer.map((item) => `<li>${formatInline(item)}</li>`).join('')}</ul>`)
    listBuffer = []
  }

  const flushCodeBlock = () => {
    html.push(`<pre><code>${codeBuffer.join('\n')}</code></pre>`)
    codeBuffer = []
  }

  for (const rawLine of lines) {
    const line = rawLine.trimEnd()

    if (line.startsWith('```')) {
      flushList()
      if (inCodeBlock) {
        flushCodeBlock()
        inCodeBlock = false
      } else {
        inCodeBlock = true
      }
      continue
    }

    if (inCodeBlock) {
      codeBuffer.push(rawLine)
      continue
    }

    if (!line.trim()) {
      flushList()
      continue
    }

    const headingMatch = line.match(/^(#{1,3})\s+(.*)$/)
    if (headingMatch) {
      flushList()
      const level = headingMatch[1].length
      html.push(`<h${level}>${formatInline(headingMatch[2])}</h${level}>`)
      continue
    }

    const listMatch = line.match(/^[-*]\s+(.*)$/)
    if (listMatch) {
      listBuffer.push(listMatch[1])
      continue
    }

    flushList()
    html.push(`<p>${formatInline(line)}</p>`)
  }

  flushList()
  if (inCodeBlock || codeBuffer.length) {
    flushCodeBlock()
  }

  return html.join('')
}
