import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Layout, Menu, Avatar, Dropdown, Space, Button, Input, message, Modal } from 'antd';
import {
  HomeOutlined,
  UserOutlined,
  LogoutOutlined,
  MoreOutlined,
  DeleteOutlined,
  LikeOutlined,
  LikeFilled,
  MessageOutlined,
  ShareAltOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import { getCurrentUser, logout } from '../api/auth';
import { getWeiboList, deleteWeibo, likeWeibo, unlikeWeibo, type Weibo } from '../api/weibo';
import { getUserInfo, type User } from '../api/user';
import './Home.css';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { Header, Content } = Layout;
const { TextArea } = Input;

const Home = () => {
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [weibos, setWeibos] = useState<Weibo[]>([]);
  const [loading, setLoading] = useState(false);
  const [publishContent, setPublishContent] = useState('');
  const [publishing, setPublishing] = useState(false);

  useEffect(() => {
    loadCurrentUser();
    loadWeibos();
  }, []);

  const loadCurrentUser = async () => {
    try {
      const user = await getCurrentUser();
      setCurrentUser(user);
      localStorage.setItem('user', JSON.stringify(user));
    } catch (error) {
      console.error('获取用户信息失败:', error);
    }
  };

  const loadWeibos = async () => {
    setLoading(true);
    try {
      const data = await getWeiboList();
      setWeibos(data.records);
    } catch (error) {
      console.error('加载微博失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handlePublish = async () => {
    if (!publishContent.trim()) {
      message.warning('请输入微博内容');
      return;
    }

    setPublishing(true);
    try {
      // TODO: 调用发布微博 API
      message.success('发布成功');
      setPublishContent('');
      loadWeibos();
    } catch (error) {
      console.error('发布失败:', error);
    } finally {
      setPublishing(false);
    }
  };

  const handleLike = async (weiboId: number, isLiked: boolean) => {
    try {
      if (isLiked) {
        await unlikeWeibo(weiboId);
      } else {
        await likeWeibo(weiboId);
      }
      loadWeibos();
    } catch (error) {
      console.error('点赞操作失败:', error);
    }
  };

  const handleDelete = (weiboId: number) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这条微博吗？',
      onOk: async () => {
        try {
          await deleteWeibo(weiboId);
          message.success('删除成功');
          loadWeibos();
        } catch (error) {
          console.error('删除失败:', error);
        }
      },
    });
  };

  const handleLogout = () => {
    Modal.confirm({
      title: '确认退出',
      content: '确定要退出登录吗？',
      onOk: () => {
        logout();
        navigate('/login');
      },
    });
  };

  const userMenu = (
    <Menu>
      <Menu.Item key="profile" onClick={() => navigate(`/user/${currentUser?.id}`)}>
        <UserOutlined /> 个人主页
      </Menu.Item>
      <Menu.Item key="logout" onClick={handleLogout}>
        <LogoutOutlined /> 退出登录
      </Menu.Item>
    </Menu>
  );

  const weiboMenu = (weibo: Weibo) => (
    <Menu>
      {currentUser?.id === weibo.userId && (
        <Menu.Item key="delete" onClick={() => handleDelete(weibo.id)} danger>
          <DeleteOutlined /> 删除
        </Menu.Item>
      )}
    </Menu>
  );

  return (
    <Layout className="app-container">
      <Header className="app-header">
        <div className="header-content">
          <a href="/" className="logo">微博</a>
          <div className="header-right">
            {currentUser && (
              <Dropdown overlay={userMenu} trigger={['click']}>
                <Space className="user-dropdown" style={{ cursor: 'pointer' }}>
                  <Avatar src={currentUser.avatar} icon={<UserOutlined />} />
                  <span>{currentUser.nickname}</span>
                </Space>
              </Dropdown>
            )}
          </div>
        </div>
      </Header>

      <Content className="main-content">
        <div className="main-layout">
          <div className="main-feed">
            {/* 发布框 */}
            <div className="publish-box">
              <TextArea
                value={publishContent}
                onChange={(e) => setPublishContent(e.target.value)}
                placeholder="有什么新鲜事想和大家分享？"
                autoSize={{ minRows: 3, maxRows: 6 }}
              />
              <div style={{ textAlign: 'right', marginTop: 12 }}>
                <Button type="primary" onClick={handlePublish} loading={publishing}>
                  发布
                </Button>
              </div>
            </div>

            {/* 微博列表 */}
            {weibos.map((weibo) => (
              <div key={weibo.id} className="weibo-card">
                <div className="weibo-header">
                  <Avatar
                    src={weibo.user?.avatar}
                    icon={<UserOutlined />}
                    size={48}
                    onClick={() => navigate(`/user/${weibo.userId}`)}
                    style={{ cursor: 'pointer' }}
                  />
                  <div className="user-info">
                    <div className="user-nickname">{weibo.user?.nickname}</div>
                    <div className="user-username">@{weibo.user?.username}</div>
                  </div>
                  <div className="weibo-time">{dayjs(weibo.createdTime).fromNow()}</div>
                  <Dropdown menu={{ items: [{ key: 'more', icon: <MoreOutlined />, label: '更多', menu: weiboMenu(weibo) }] }} trigger={['click']}>
                    <Button type="text" icon={<MoreOutlined />} />
                  </Dropdown>
                </div>

                <div className="weibo-content">{weibo.content}</div>

                {weibo.images && weibo.images.length > 0 && (
                  <div className="weibo-images">
                    {weibo.images.map((img, index) => (
                      <img key={index} src={img} alt="" className="weibo-image" />
                    ))}
                  </div>
                )}

                <div className="weibo-footer">
                  <div
                    className={`weibo-action ${weibo.isLiked ? 'liked' : ''}`}
                    onClick={() => handleLike(weibo.id, !!weibo.isLiked)}
                  >
                    {weibo.isLiked ? <LikeFilled /> : <LikeOutlined />}
                    <span>{weibo.likeCount}</span>
                  </div>
                  <div className="weibo-action" onClick={() => navigate(`/weibo/${weibo.id}`)}>
                    <MessageOutlined />
                    <span>{weibo.commentCount}</span>
                  </div>
                  <div className="weibo-action">
                    <ShareAltOutlined />
                    <span>{weibo.repostCount}</span>
                  </div>
                </div>
              </div>
            ))}

            {weibos.length === 0 && !loading && (
              <div className="empty-state">
                <div className="empty-state-icon">📝</div>
                <div>暂无微博，快来发布第一条吧！</div>
              </div>
            )}
          </div>

          {/* 侧边栏 */}
          <div className="sidebar">
            <div className="sidebar-card">
              <div className="sidebar-title">热门话题</div>
              <div className="hot-topics">
                <div className="topic-item">#今天天气真好#</div>
                <div className="topic-item">#美食分享#</div>
                <div className="topic-item">#程序员日常#</div>
              </div>
            </div>
          </div>
        </div>
      </Content>
    </Layout>
  );
};

export default Home;
