import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Avatar, Button, Space, Tabs, message, Modal, Card } from 'antd';
import {
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
    <div className="user-profile-page">
      {/* 用户资料头部 */}
      <Card className="user-profile-header" size="small">
        <div style={{ textAlign: 'center', padding: '20px 0' }}>
          <Avatar
            src={profileUser.avatar}
            icon={<UserOutlined />}
            size={100}
            className="profile-avatar"
            style={{ marginBottom: 16 }}
          />
          <h1 className="profile-nickname" style={{ margin: '12px 0 8px', fontSize: 24 }}>
            {profileUser.nickname}
          </h1>
          <div className="profile-username" style={{ color: '#999', marginBottom: 12 }}>
            @{profileUser.username}
          </div>
          {profileUser.introduction && (
            <div className="profile-introduction" style={{ color: '#666', marginBottom: 16 }}>
              {profileUser.introduction}
            </div>
          )}
          
          {!isCurrentUser && (
            <Button
              type={isFollowing ? 'default' : 'primary'}
              loading={followLoading}
              onClick={handleFollow}
              size="large"
              style={{ minWidth: 100 }}
            >
              {isFollowing ? '已关注' : '关注'}
            </Button>
          )}

          <div className="profile-stats" style={{ display: 'flex', justifyContent: 'center', gap: 32, marginTop: 24, paddingTop: 24, borderTop: '1px solid #f0f0f0' }}>
            <div className="stat-item" style={{ textAlign: 'center' }}>
              <div className="stat-value" style={{ fontSize: 20, fontWeight: 600 }}>
                {profileUser.weiboCount || 0}
              </div>
              <div className="stat-label" style={{ color: '#999', fontSize: 12 }}>微博</div>
            </div>
            <div className="stat-item" style={{ textAlign: 'center' }}>
              <div className="stat-value" style={{ fontSize: 20, fontWeight: 600 }}>
                {profileUser.followingCount || 0}
              </div>
              <div className="stat-label" style={{ color: '#999', fontSize: 12 }}>关注</div>
            </div>
            <div className="stat-item" style={{ textAlign: 'center' }}>
              <div className="stat-value" style={{ fontSize: 20, fontWeight: 600 }}>
                {profileUser.followerCount || 0}
              </div>
              <div className="stat-label" style={{ color: '#999', fontSize: 12 }}>粉丝</div>
            </div>
          </div>
        </div>
      </Card>

      {/* 微博列表 */}
      <Card className="profile-weibos" size="small" style={{ marginTop: 16 }}>
        <Tabs
          defaultActiveKey="weibos"
          items={[
            {
              key: 'weibos',
              label: `微博 (${weibos.length})`,
              children: (
                <div className="weibo-list">
                  {weibos.map((weibo) => (
                    <Card key={weibo.id} className="weibo-card" size="small" style={{ marginBottom: 16 }}>
                      <div className="weibo-content" style={{ marginBottom: 12, lineHeight: 1.6 }}>
                        {weibo.content}
                      </div>

                      {weibo.images && weibo.images.length > 0 && (
                        <div className="weibo-images" style={{ display: 'flex', gap: '8px', marginBottom: 12, flexWrap: 'wrap' }}>
                          {weibo.images.map((img, index) => (
                            <img 
                              key={index} 
                              src={img} 
                              alt="" 
                              className="weibo-image"
                              style={{ width: 100, height: 100, objectFit: 'cover', borderRadius: 4, cursor: 'pointer' }}
                            />
                          ))}
                        </div>
                      )}

                      <div className="weibo-time" style={{ color: '#999', fontSize: 12, marginBottom: 12 }}>
                        {dayjs(weibo.createdTime).format('YYYY-MM-DD HH:mm:ss')}
                      </div>

                      <div className="weibo-footer" style={{ display: 'flex', justifyContent: 'space-around', paddingTop: 12, borderTop: '1px solid #f0f0f0' }}>
                        <div
                          className={`weibo-action ${weibo.isLiked ? 'liked' : ''}`}
                          onClick={() => handleLike(weibo.id, !!weibo.isLiked)}
                          style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4, color: weibo.isLiked ? '#ff4d4f' : '#666' }}
                        >
                          {weibo.isLiked ? <LikeFilled /> : <LikeOutlined />}
                          <span>{weibo.likeCount}</span>
                        </div>
                        <div 
                          className="weibo-action" 
                          onClick={() => navigate(`/weibo/${weibo.id}`)}
                          style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4, color: '#666' }}
                        >
                          <MessageOutlined />
                          <span>{weibo.commentCount}</span>
                        </div>
                        <div 
                          className="weibo-action"
                          style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4, color: '#666' }}
                        >
                          <ShareAltOutlined />
                          <span>{weibo.repostCount}</span>
                        </div>
                        {isCurrentUser && (
                          <div
                            className="weibo-action"
                            onClick={() => handleDelete(weibo.id)}
                            style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 4, color: '#ff4d4f' }}
                          >
                            <DeleteOutlined />
                            <span>删除</span>
                          </div>
                        )}
                      </div>
                    </Card>
                  ))}

                  {weibos.length === 0 && !loading && (
                    <div className="empty-state" style={{ textAlign: 'center', padding: 40 }}>
                      <div className="empty-state-icon" style={{ fontSize: 48, marginBottom: 12 }}>📝</div>
                      <div style={{ color: '#999' }}>暂无微博</div>
                    </div>
                  )}
                </div>
              ),
            },
          ]}
        />
      </Card>
    </div>
  );
};

export default UserProfile;
