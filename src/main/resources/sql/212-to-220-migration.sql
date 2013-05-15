alter table f_file add column (is_hot tinyint not null default 0,is_new tinyint not null default 0);

update f_file set dtype='2' where  ux_size is null;

create table f_tag(
  id int auto_increment,
  name varchar(35) not null,
  primary key(id)
)ENGINE=InnoDB;

insert into f_tag(name) values ("男");
insert into f_tag(name) values ("女");
insert into f_tag(name) values ("隐藏");

create table f_file_tag(
  file_id int not null,
  tag_id int not null,
  primary key(file_id,tag_id)
)ENGINE=InnoDB;