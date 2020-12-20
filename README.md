notebook
========

Android notebook. 期中实验报告：
  实验要求：
  基本要求：
 NoteList中显示条目增加时间戳显示
 添加笔记查询功能（根据标题查询）
  附加功能：根据自身实际情况进行扩充，以下是建议的扩展功能（拟作
为期末作业）
 UI美化
 更改记事本的背景
 导出笔记，笔记的云备份和恢复
 添加代办功能
 记事本的设置的功能
 笔记分类
 支持多种资源，如保存图片、语音、视频等（参考印象笔记）
 语音搜索？
 笔记便签

1.增加时间戳显示
NoteList使用SimpleCursorAdapter来装配数据，首先查询数据库的内容

 Cursor cursor = managedQuery(
         getIntent().getData(),           
         PROJECTION,                      
         null,                             
         null,                             
         NotePad.Notes.DEFAULT_SORT_ORDER);

然后通过SimpleCursorAdapter来进行装配

 SimpleCursorAdapter adapter
         = new SimpleCursorAdapter(
                   this,                             
                   R.layout.noteslist_item,          
                   cursor,                           
                   dataColumns,
                   viewIDs);
添加时间戳的位置在主页面的每个列表项中添加，即在notelist_item.xml布局文件中添加

实验截图：
