import fs from 'node:fs'
import path from 'node:path'
import crypto from 'node:crypto'
import { execFileSync } from 'node:child_process'

const root = path.resolve(process.argv[2] || '.')
const destination = path.resolve(process.argv[3] || 'docs/schema-model.json')
const repo = 'https://github.com/seonyoungyoo16/springboot-board-lab'
const java = 'src/main/java/dev/portfolio/board/'
const source = file => fs.readFileSync(path.join(root, file), 'utf8')
const lineOf = (file, term) => { const lines = source(file).split('\n'); const i = lines.findIndex(l => l.includes(term)); if (i < 0) throw Error(`Missing source evidence: ${file} / ${term}`); return i + 1 }
let revision = null
try { revision = execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim() } catch {}
const sqlFile = 'src/main/resources/schema.sql', sql = source(sqlFile)
const entities = ['AppUser', 'Post', 'Comment', 'PostLike', 'PostImage'].map(name => { const file = java + name + '.java', content = source(file); return { name, file, table: content.match(/@Table\(name = "([^"]+)"/)?.[1], line: lineOf(file, '@Table') } })
const tables = [...sql.matchAll(/CREATE TABLE IF NOT EXISTS (\w+) \(([\s\S]*?)\n\);/g)].map(match => {
  const name = match[1], block = match[2], entity = entities.find(e => e.table === name)
  if (!entity) throw Error('SQL table has no JPA mapping: ' + name)
  const columns = block.split('\n').map(l => l.trim().replace(/,$/, '')).filter(l => l && !l.startsWith('CONSTRAINT')).map(l => { const m = l.match(/^(\w+)\s+(BIGINT|BLOB|VARCHAR\(\d+\)|TIMESTAMP WITH TIME ZONE)(.*)$/); if (!m) throw Error('Unsupported DDL column: ' + l); return { name: m[1], type: m[2], primary: m[3].includes('PRIMARY KEY'), nullable: !m[3].includes('NOT NULL') && !m[3].includes('PRIMARY KEY') } })
  const relations = [...block.matchAll(/CONSTRAINT (\w+) FOREIGN KEY \((\w+)\) REFERENCES (\w+)\((\w+)\)/g)].map(m => ({ name: m[1], from: name, column: m[2], to: m[3], referencedColumn: m[4], cardinality: 'N:1', line: lineOf(sqlFile, m[1]) }))
  const unique = [...block.matchAll(/CONSTRAINT (\w+) UNIQUE \(([^)]+)\)/g)].map(m => ({ name: m[1], columns: m[2].split(',').map(x => x.trim()) }))
  return { name, entity: entity.name, columns, relations, unique, source: { file: sqlFile, line: lineOf(sqlFile, 'CREATE TABLE IF NOT EXISTS ' + name), entityFile: entity.file, entityLine: entity.line } }
})
const controllerFile = java + 'BoardController.java', controller = source(controllerFile), serviceFile = java + 'BoardService.java', service = source(serviceFile)
const endpoints = []
let mapping = null
controller.split('\n').forEach((line, index) => {
  const m = line.match(/@(Get|Post|Put|Delete)Mapping\((?:value = )?"([^"]+)"/)
  if (m) mapping = { method: m[1].toUpperCase(), path: '/api' + m[2] }
  if (mapping && /public .*\(/.test(line)) { endpoints.push({...mapping, source: {file: controllerFile, line: index + 1}}); mapping = null }
})
const methodLine = method => { const lines = service.split('\n'), i = lines.findIndex(l => new RegExp('public .*\\b' + method + '\\(').test(l)); if (i < 0) throw Error('Missing service method: ' + method); return i + 1 }
const operation = (id, title, method, tables, labels, evidence) => {
  const line = methodLine(method)
  for (const fragment of evidence) lineOf(serviceFile, fragment)
  return { id, title, method, tables, steps: labels.map((label, i) => ({ label, table: tables[Math.min(i, tables.length - 1)] || null })), source: { file: serviceFile, line }, evidence: evidence.map(fragment => ({ file: serviceFile, line: lineOf(serviceFile, fragment), code: fragment })) }
}
const operations = [
  operation('create', '글 작성', 'create', ['app_users', 'posts'], ['작성자 조회', '게시글 INSERT'], ['new Post(user(actor)', 'posts.saveAndFlush']),
  operation('update', '글 수정', 'update', ['app_users', 'posts'], ['작성자 확인', '버전 확인 후 UPDATE'], ['owner(p.author.getId(), actor)', '!p.version.equals(version)', 'p.title = title.trim()']),
  operation('delete', '글 삭제', 'delete', ['post_images', 'comments', 'post_likes', 'posts'], ['이미지 DELETE', '댓글 DELETE', '좋아요 DELETE', '게시글 DELETE · commit'], ['images.deleteByPostId(id)', 'comments.deleteByPostId(id)', 'likes.deleteByPostId(id)', 'posts.delete(p)']),
  operation('comment', '댓글 작성', 'addComment', ['posts', 'app_users', 'comments'], ['게시글 확인·잠금', '작성자 조회', '댓글 INSERT'], ['new Comment(p, user(actor), body.trim())']),
  operation('commentUpdate', '댓글 수정', 'updateComment', ['posts', 'app_users', 'comments'], ['소속 게시글 확인', '작성자 확인', '댓글 UPDATE'], ['comment(id, commentId)', 'c.body = body.trim()']),
  operation('commentDelete', '댓글 삭제', 'deleteComment', ['posts', 'app_users', 'comments'], ['소속 게시글 확인', '작성자 확인', '댓글 DELETE'], ['comments.delete(c)']),
  operation('like', '좋아요 등록', 'like', ['posts', 'app_users', 'post_likes'], ['게시글 확인·잠금', '사용자 조회', '중복 확인 후 INSERT'], ['!likes.existsByPostIdAndUserId(id, actor)', 'new PostLike(p, u)']),
  operation('unlike', '좋아요 취소', 'unlike', ['posts', 'post_likes'], ['게시글 확인·잠금', '현재 사용자 좋아요 DELETE'], ['likes.findByPostIdAndUserId(id, actor).ifPresent(likes::delete)']),
  operation('createMultipart', '데이터 + 이미지 업로드', 'createMultipart', ['app_users', 'posts', 'post_images'], ['JSON data + image 검증 · 사용자 조회', '게시글 INSERT', '이미지 BLOB INSERT'], ['validateImage(image)', 'images.saveAndFlush(new PostImage(p, filename, image.getContentType(), bytes))']),
  operation('readImage', '이미지 조회', 'readImage', ['post_images'], ['이미지 SELECT → 바이너리 응답'], ['images.findById(id)']),
]
const size = field => { const m = controller.match(new RegExp('@Size\\(max = (\\d+)\\) String ' + field)); if (!m) throw Error('No validation ' + field); return Number(m[1]) }
const commentLimit = Number(controller.match(/record CommentInput\(@NotBlank @Size\(max = (\d+)\)/)?.[1])
if (!commentLimit) throw Error('No comment limit')
const model = { repo, revision, title: 'Spring Boot Board Lab', tables, relations: tables.flatMap(t => t.relations), endpoints, operations, rules: { titleMax: size('title'), bodyMax: size('body'), commentMax: commentLimit, ownership: service.includes('작성자만 변경'), versionCheck: service.includes('!p.version.equals(version)'), uniqueLike: tables.find(t => t.name === 'post_likes').unique[0].columns, deleteOrder: ['post_images', 'comments', 'post_likes', 'posts'] }, sources: [sqlFile, controllerFile, serviceFile, java + 'Repositories.java', ...entities.map(e => e.file)].map(file => ({ file, sha256: crypto.createHash('sha256').update(source(file)).digest('hex') })) }
fs.mkdirSync(path.dirname(destination), { recursive: true }); fs.writeFileSync(destination, JSON.stringify(model, null, 2) + '\n')
console.log(JSON.stringify({ destination, revision, tables: tables.length, foreignKeys: model.relations.length, endpoints: endpoints.length, operations: operations.length }))
