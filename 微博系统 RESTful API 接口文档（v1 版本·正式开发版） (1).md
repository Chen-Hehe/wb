# 微博系统 RESTful API 接口文档（v1 版本·正式开发版）

# 文档说明

## 1\. 基础信息

- **接口前缀**：所有接口统一前缀为 `/api/v1`

- **数据格式**：除图片上传接口外，请求与响应均采用 `application/json` 格式

- **鉴权规则**：需登录后访问的接口，需在请求头携带 `Authorization: Bearer \{jwt\_token\}`（token 由登录接口返回）

- **数据库关联**：接口字段与数据库表（users、weibos、comments、attentions）字段一一对应，无额外冗余字段（字段已去除前缀）

## 2\. 统一响应结构

```json
{
  "code": 200,      // 状态码（详情见下方说明）
  "msg": "操作成功", // 提示信息（失败时返回错误原因）
  "data": {}        // 业务数据（成功时返回，失败时可为null）
}
```

## 3\. 状态码说明

|状态码|含义|常见场景|
|---|---|---|
|200|操作成功|登录、注册、发布微博、关注等正常操作|
|400|参数错误|必填参数缺失、参数格式错误（如手机号、密码长度不符）|
|401|未授权|未携带token、token过期、token无效|
|403|无权限|删除他人微博、评论，关注已关注用户等非法操作|
|404|资源不存在|访问不存在的微博、用户、评论|
|500|服务器异常|数据库操作失败、接口内部报错|

# 二、用户模块（对应数据库表：users）

## 1\. 用户注册

- **接口URL**：`POST /api/v1/users/register`

- **请求方式**：POST

- **鉴权要求**：无需鉴权

- **数据库关联**：插入数据到 users 表，生成 id（自增）

- **请求体参数**：

|参数名|类型|必填|长度限制|说明|数据库对应字段|
|---|---|---|---|---|---|
|loginname|string|是|1\-20字符|用户登录账号，唯一不可重复|user\_loginname|
|loginpwd|string|是|1\-20字符|用户登录密码，明文传输（实际开发建议加密）|user\_loginpwd|
|nickname|string|是|1\-20字符|用户昵称，可重复|user\_nickname|

**请求示例**：

```json
{
  "loginname": "zhangsan",
  "loginpwd": "123456",
  "nickname": "张三"
}
```

**响应示例**：

```json
{
  "code": 200,
  "msg": "注册成功",
  "data": {
    "id": 8,          // 新注册用户ID（自增）
    "loginname": "zhangsan",
    "nickname": "张三"
  }
}
```

## 2\. 用户登录（获取JWT令牌）

- **接口URL**：`POST /api/v1/users/login`

- **请求方式**：POST

- **鉴权要求**：无需鉴权

- **数据库关联**：查询 users 表，匹配 loginname 和 loginpwd

- **请求体参数**：

|参数名|类型|必填|说明|数据库对应字段|
|---|---|---|---|---|
|loginname|string|是|用户登录账号|user\_loginname|
|loginpwd|string|是|用户登录密码|user\_loginpwd|

**请求示例**：

```json
{
  "loginname": "zhangsan",
  "loginpwd": "123456"
}
```

**响应示例**：

```json
{
  "code": 200,
  "msg": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoxLCJ1c2VyX25pY2tuYW1lIjoi5byg5LiJIiwiaWF0IjoxNzE0NzY1MjAwLCJleHAiOjE3MTQ3Njg4MDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
    "id": 1,
    "nickname": "张三",
    "photo": "7.jpg",       // 用户头像路径（数据库默认值）
    "score": 240103100,    // 用户积分（数据库默认值）
    "attionCount": 108     // 用户关注数（数据库默认值）
  }
}
```

## 3\. 获取当前登录用户信息

- **接口URL**：`GET /api/v1/users/current`

- **请求方式**：GET

- **鉴权要求**：需鉴权（携带Authorization请求头）

- **数据库关联**：根据token解析的id，查询users表完整信息

- **请求参数**：无（仅需请求头）

**请求头示例**：

```plain
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoxLCJ1c2VyX25pY2tuYW1lIjoi5byg5LiJIiwiaWF0IjoxNzE0NzY1MjAwLCJleHAiOjE3MTQ3Njg4MDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

**响应示例**：

```json
{
  "code": 200,
  "msg": "查询成功",
  "data": {
    "id": 1,
    "nickname": "张三",
    "loginname": "zhangsan",
    "photo": "7.jpg",
    "score": 240103100,
    "attionCount": 108
  }
}
```

## 4\. 根据ID获取用户信息

- **接口URL**：`GET /api/v1/users/\{userId\}`

- **请求方式**：GET

- **鉴权要求**：无需鉴权

- **数据库关联**：根据路径参数userId，查询users表对应记录

- **请求参数**：

|参数名|类型|必填|位置|说明|数据库对应字段|
|---|---|---|---|---|---|
|userId|int|是|路径参数|目标用户ID|user\_id|

**请求示例**：`GET /api/v1/users/1`

**响应示例**：

```json
{
  "code": 200,
  "msg": "查询成功",
  "data": {
    "id": 1,
    "nickname": "张三",
    "photo": "7.jpg",
    "score": 240103100,
    "attionCount": 108
  }
}
```

# 三、微博模块（对应数据库表：weibos）

## 1\. 发布微博

- **接口URL**：`POST /api/v1/weibos`

- **请求方式**：POST

- **鉴权要求**：需鉴权

- **数据库关联**：插入数据到 weibos 表，userid 为当前登录用户ID

- **请求体参数**：

|参数名|类型|必填|长度限制|说明|数据库对应字段|
|---|---|---|---|---|---|
|title|string|是|1\-200字符|微博标题|wb\_title|
|content|string|是|1\-2000字符|微博内容（可包含HTML标签，如数据库示例）|wb\_content|
|img|string|否|1\-100字符|微博图片路径（由图片上传接口返回），无图片则为null|wb\_img|

**请求示例**：

```json
{
  "title": "一个博物馆就是一所大学校",
  "content": "<p>走进博物馆，了解历史文化遗产保护工作。一次次触摸历史，一次次寻访传统</p>",
  "img": "c3.jpg"
}
```

**响应示例**：

```json
{
  "code": 200,
  "msg": "发布成功",
  "data": {
    "id": 70,                  // 新发布微博ID（自增）
    "userid": 1,               // 当前登录用户ID
    "title": "一个博物馆就是一所大学校",
    "createtime": "2024-05-03 14:30:00" // 系统自动生成
  }
}
```

## 2\. 微博列表（分页 \+ 搜索）

- **接口URL**：`GET /api/v1/weibos`

- **请求方式**：GET

- **鉴权要求**：无需鉴权

- **数据库关联**：查询 weibos 表，支持按 title、content 模糊搜索，分页返回

- **请求参数（查询参数）**：

|参数名|类型|必填|默认值|说明|
|---|---|---|---|---|
|pageNum|int|是|1|页码|
|pageSize|int|是|10|每页显示条数|
|keyword|string|否|null|搜索关键词，模糊匹配微博标题和内容|

**请求示例**：`GET /api/v1/weibos?pageNum=1\&amp;pageSize=5\&amp;keyword=博物馆`

**响应示例**：

```json
{
  "code": 200,
  "msg": "查询成功",
  "data": {
    "total": 1,                  // 符合条件的总微博数
    "pages": 1,                  // 总页数
    "pageNum": 1,
    "pageSize": 5,
    "list": [
      {
        "id": 59,
        "userid": 1,
        "title": "一个博物馆就是一所大学校",
        "content": "<p>走进博物馆，了解历史文化遗产保护工作。一次次触摸历史，一次次寻访传统</p>",
        "createtime": "2022-05-18 23:08:42",
        "readcount": 0,
        "img": "c3.jpg",
        "user_info": {            // 关联查询发布者信息
          "id": 1,
          "nickname": "张三",
          "photo": "7.jpg"
        }
      }
    ]
  }
}
```

## 3\. 微博详情

- **接口URL**：`GET /api/v1/weibos/\{wbId\}`

- **请求方式**：GET

- **鉴权要求**：无需鉴权

- **数据库关联**：根据 wbId 查询 weibos 表，同时关联 users 表获取发布者信息，访问时自动将 readcount \+1

- **请求参数**：

|参数名|类型|必填|位置|说明|数据库对应字段|
|---|---|---|---|---|---|
|wbId|int|是|路径参数|微博ID|wb\_id|

**请求示例**：`GET /api/v1/weibos/59`

**响应示例**：

```json
{
  "code": 200,
  "msg": "查询成功",
  "data": {
    "id": 59,
    "userid": 1,
    "title": "一个博物馆就是一所大学校",
    "content": "<p>走进博物馆，了解历史文化遗产保护工作。一次次触摸历史，一次次寻访传统</p>",
    "createtime": "2022-05-18 23:08:42",
    "readcount": 1,           // 访问后自动+1
    "img": "c3.jpg",
    "user_info": {
      "id": 1,
      "nickname": "张三",
      "photo": "7.jpg",
      "attionCount": 108
    }
  }
}
```

## 4\. 删除自己的微博

- **接口URL**：`DELETE /api/v1/weibos/\{wbId\}`

- **请求方式**：DELETE

- **鉴权要求**：需鉴权

- **数据库关联**：根据 wbId 删除 weibos 表对应记录，需验证 userid 与当前登录用户ID一致

- **请求参数**：

|参数名|类型|必填|位置|说明|
|---|---|---|---|---|
|wbId|int|是|路径参数|要删除的微博ID，需为当前用户发布|

**请求示例**：`DELETE /api/v1/weibos/70`

**响应示例**：

```json
{
  "code": 200,
  "msg": "删除成功",
  "data": null
}
```

## 5\. 增加微博阅读量（独立接口，可选）

- **接口URL**：`PATCH /api/v1/weibos/\{wbId\}/read\-count`

- **请求方式**：PATCH

- **鉴权要求**：无需鉴权

- **数据库关联**：根据 wbId 更新 weibos 表，将 readcount 字段 \+1

- **请求参数**：路径参数 wbId（微博ID）

**请求示例**：`PATCH /api/v1/weibos/59/read\-count`

**响应示例**：

```json
{
  "code": 200,
  "msg": "阅读量增加成功",
  "data": {
    "id": 59,
    "readcount": 2
  }
}
```

# 四、图片上传模块

## 1\. 上传图片

- **接口URL**：`POST /api/v1/upload/images`

- **请求方式**：POST

- **鉴权要求**：需鉴权

- **数据格式**：`multipart/form\-data`

- **数据库关联**：图片路径用于填充 weibos 表的 img 字段

- **请求参数**：

|参数名|类型|必填|说明|备注|
|---|---|---|---|---|
|file|file|是|图片文件|支持jpg、png格式，大小不超过5MB|

**请求示例**：form\-data 格式上传 file 文件

**响应示例**：

```json
{
  "code": 200,
  "msg": "上传成功",
  "data": {
    "imgUrl": "c3.jpg"  // 图片存储路径（与数据库weibos表img字段一致）
  }
}
```

# 五、评论模块（对应数据库表：comments）

## 1\. 发表评论

- **接口URL**：`POST /api/v1/weibos/\{wbId\}/comments`

- **请求方式**：POST

- **鉴权要求**：需鉴权

- **数据库关联**：插入数据到 comments 表，weiboid 为路径参数 wbId，userid 为当前登录用户ID

- **请求参数**：

|参数名|类型|必填|位置|长度限制|说明|数据库对应字段|
|---|---|---|---|---|---|---|
|wbId|int|是|路径参数|\-|目标微博ID|cm\_weiboid|
|content|string|是|请求体|1\-2000字符|评论内容（可包含HTML标签、表情）|cm\_content|

**请求示例**：`POST /api/v1/weibos/59/comments`

```json
{
  "content": "<p>很好<img alt=\"wink\" height=\"23\" src=\"http://localhost:8080/xw/static/ckeditor/plugins/smiley/images/wink_smile.png\" title=\"wink\" width=\"23\" /></p>"
}
```

> （注：文档部分内容可能由 AI 生成）
