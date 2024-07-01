create table chart
(
    id          bigint auto_increment comment 'id'
        primary key,
    goal        text                                   null comment '分析目标',
    chartData   text                                   null comment '图表数据',
    chartType   varchar(512)                           null comment '图表类型',
    genChart    text                                   null comment '生成的图标数据',
    genResult   text                                   null comment '生成的分析结论',
    createTime  datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint      default 0                 not null comment '是否删除',
    userId      bigint                                 not null comment '创建图表的用户id',
    state       varchar(128) default 'wait'            not null comment '图表的状态(wait,running,succeed,fail)',
    name        varchar(64)                            not null comment '图表名称',
    execMessage text                                   null comment '执行信息'
)
    comment '图表信息表' collate = utf8mb4_unicode_ci;

create table image
(
    id          bigint auto_increment comment 'id'
        primary key,
    goal        text                                  null comment '分析目标',
    imageType   varchar(512)                          null comment '图片类型',
    genResult   text                                  null comment '生成的分析结论',
    createTime  datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint     default 0                 not null comment '是否删除',
    baseString  text                                  not null comment '图片的base64编码',
    state       varchar(64) default 'wait'            not null comment 'ai分析图片状态',
    execMessage varchar(512)                          null comment '执行信息'
)
    comment '图片分析表' collate = utf8mb4_unicode_ci;

create table user
(
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除'
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_unionAccount
    on user (userAccount);

