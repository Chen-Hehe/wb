import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Layout, Avatar, Button, Space, Tabs, message, Modal } from 'antd';
import {
  HomeOutlined,
  UserOutlined,
  LikeOutlined,
  LikeFilled,
  MessageOutlined,
  ShareAltOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import { getCurrentUser } from '../api/auth';
import { getUserWeiboList, deleteWeibo, likeWeibo, unlikeWeibo, type Weibo } from '../api/weibo';
import { getUserInfo, followUser, unfollowUser, checkFollowing, type User } from '../api/user';
import './UserProfile.css';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const { Header, Content } = Layout;

const UserProfile = () => {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [profileUser, setProfileUser] = useState<User | null>(null);
  const [weibos, setWeibos] = useState<Weibo[]>([]);
  const [loading, setLoading] = useState(false);
  const [isFollowing, setIsFollowing] = useState(false);
  const [followLoading, setFollowLoading] = useState(false);

  useEffect(() => {
    loadCurrentUser();
    loadUserProfile();
    loadUserWeibos();
  }, [userId]);

  const loadCurrentUser = async () => {
    try {
      const user = await getCurrentUser();
      setCurrentUser(user);
    } catch (error) {
      console.error('获取当前用户失败:', error);
    }
  };

  const loadUserProfile = async () => {
    try {
      const user = await getUserInfo(Number(userId));
      setProfileUser(user);
      
      // 检查是否关注（如果不是自己）
      if (currentUser && currentUser.id !== Number(userId)) {
        const following = await checkFollowing(Number(userId));
        setIsFollowing(following);
      }
    } catch (error) {
      console.error('获取用户资料失败:', error);
    }
  };

  const loadUserWeibos = async () => {
    setLoading(true);
    try {
      const data = await getUserWeiboList(Number(userId));
      setWeibos(data.records);
    } catch (error) {
      console.error('加载微博失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleFollow = async () => {
    if (!userId) return;
    setFollowLoading(true);
    try {
      if (isFollowing) {
        await unfollowUser(Number(userId));
        setIsFollowing(false);
        message.success('已取消关注');
      } else {
        await followUser(Number(userId));
        setIsFollowing(true);
        message.success('关注成功');
      }
      loadUserProfile();
    } catch (error) {
      console.error('关注操作失败:', error);
    } finally {
      setFollowLoading(false);
    }
  };

  const handleLike = async (weiboId: number, isLiked: boolean) => {
    try {
      if (isLiked) {
        await unlikeWeibo(weiboId);
      } else {
        await likeWeibo(weiboId);
      }
      loadUserWeibos();
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
          loadUserWeibos();
        } catch (error) {
          console.error('删除失败:', error);
        }
      },
    });
  };

  if (!profileUser) {
    return <div className="loading">加载中...</div>;
  }

  const isCurrentUser = currentUser?.id === profileUser.id;

  return (
    <Layout className="app-container">
      <Header className="app-header">
        <div className="header-content">
          <a href="/" className="logo">微博</a>
          <Button icon={<HomeOutlined />} onClick={() => navigate('/')}>
            返回首页
          </Button>
        </div>
      </Header>

      <Content className="profile-content">
        <div className="profile-container">
          {/* 用户资料头部 */}
          <div className="user-profile-header">
            <Avatar
              src={profileUser.avatar}
              icon={<UserOutlined />}
              size={100}
              className="profile-avatar"
            />
            <h1 className="profile-nickname">{profileUser.nickname}</h1>
            <div className="profile-username">@{profileUser.username}</div>
            {profileUser.introduction && (
              <div className="profile-introduction">{profileUser.introduction}</div>
            )}
            
            {!isCurrentUser && (
              <Button
                type={isFollowing ? 'default' : 'primary'}
                loading={followLoading}
                onClick={handleFollow}
                size="large"
              >
                {isFollowing ? '已关注' : '关注'}
              </Button>
            )}

            <div className="profile-stats">
              <div className="stat-item">
                <div className="stat-value">{profileUser.weiboCount || 0}</div>
                <div className="stat-label">微博</div>
              </div>
              <div className="stat-item">
                <div className="stat-value">{profileUser.followingCount || 0}</div>
                <div className="stat-label">关注</div>
              </div>
              <div className="stat-item">
                <div className="stat-value">{profileUser.followerCount || 0}</div>
                <div className="stat-label">粉丝</div>
              </div>
            </div>
          </div>

          {/* 微博列表 */}
          <div className="profile-weibos">
            <Tabs
              defaultActiveKey="weibos"
              items={[
                {
                  key: 'weibos',
                  label: `微博 (${weibos.length})`,
                  children: (
                    <div className="weibo-list">
                      {weibos.map((weibo) => (
                        <div key={weibo.id} className="weibo-card">
                          <div className="weibo-content">{weibo.content}</div>

                          {weibo.images && weibo.images.length > 0 && (
                            <div className="weibo-images">
                              {weibo.images.map((img, index) => (
                                <img key={index} src={img} alt="" className="weibo-image" />
                              ))}
                            </div>
                          )}

                          <div className="weibo-time">
                            {dayjs(weibo.createdTime).format('YYYY-MM-DD HH:mm:ss')}
                          </div>

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
                            {isCurrentUser && (
                              <div
                                className="weibo-action"
                                onClick={() => handleDelete(weibo.id)}
                              >
                                <DeleteOutlined />
                              </div>
                            )}
                          </div>
                        </div>
                      ))}

                      {weibos.length === 0 && !loading && (
                        <div className="empty-state">
                          <div className="empty-state-icon">📝</div>
                          <div>暂无微博</div>
                        </div>
                      )}
                    </div>
                  ),
                },
              ]}
            />
          </div>
        </div>
      </Content>
    </Layout>
  );
};

export default UserProfile;
