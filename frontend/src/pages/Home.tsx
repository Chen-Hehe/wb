import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Avatar, Dropdown, Space, Button, Input, message, Modal, Card } from 'antd';
import {
  MoreOutlined,
  DeleteOutlined,
  LikeOutlined,
  LikeFilled,
  MessageOutlined,
  ShareAltOutlined,
  SendOutlined,
  UserOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import { getCurrentUser } from '../api/auth';
import { getWeiboList, deleteWeibo, likeWeibo, unlikeWeibo, publishWeibo, type Weibo } from '../api/weibo';
import { getUserInfo, type User } from '../api/user';
import './Home.css';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

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
      setWeibos(data.records || []);
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
      await publishWeibo({
        title: '新鲜事',
        content: publishContent,
      });
      message.success('发布成功');
      setPublishContent('');
      loadWeibos();
    } catch (error: any) {
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

  const weiboMenu = (weibo: Weibo) => ({
    items: currentUser?.id === weibo.userId
      ? [{ key: 'delete', icon: <DeleteOutlined />, label: '删除', danger: true, onClick: () => handleDelete(weibo.id) }]
      : [],
  });

  return (
    <div className="home-container">
      {/* 发布框 */}
      <Card className="publish-box" size="small">
        <div style={{ display: 'flex', gap: '12px' }}>
          <Avatar 
            src={currentUser?.avatar} 
            icon={<UserOutlined />} 
            size={48}
            onClick={() => navigate(`/user/${currentUser?.id}`)}
            style={{ cursor: 'pointer' }}
          />
          <div style={{ flex: 1 }}>
            <TextArea
              value={publishContent}
              onChange={(e) => setPublishContent(e.target.value)}
              placeholder="有什么新鲜事想和大家分享？"
              autoSize={{ minRows: 3, maxRows: 6 }}
              style={{ resize: 'none' }}
            />
            <div style={{ textAlign: 'right', marginTop: 12 }}>
              <Button type="primary" icon={<SendOutlined />} onClick={handlePublish} loading={publishing}>
                发布
              </Button>
            </div>
          </div>
        </div>
      </Card>

      {/* 微博列表 */}
      <div className="weibo-list">
        {loading ? (
          <div style={{ textAlign: 'center', padding: '40px' }}>加载中...</div>
        ) : weibos.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '40px', color: '#999' }}>
            暂无微博，快来发布第一条吧！
          </div>
        ) : (
          weibos.map((weibo) => (
            <Card key={weibo.id} className="weibo-card" size="small">
              <div className="weibo-header">
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: 1 }}>
                  <Avatar
                    src={weibo.user?.avatar}
                    icon={<UserOutlined />}
                    size={48}
                    onClick={() => navigate(`/user/${weibo.userId}`)}
                    style={{ cursor: 'pointer' }}
                  />
                  <div>
                    <div 
                      className="user-nickname" 
                      style={{ fontWeight: 'bold', cursor: 'pointer' }}
                      onClick={() => navigate(`/user/${weibo.userId}`)}
                    >
                      {weibo.user?.nickname}
                    </div>
                    <div className="user-username" style={{ color: '#999', fontSize: '12px' }}>
                      @{weibo.user?.username}
                    </div>
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <span style={{ color: '#999', fontSize: '12px' }}>
                    {dayjs(weibo.createdTime).fromNow()}
                  </span>
                  <Dropdown menu={weiboMenu(weibo)} trigger={['click']} placement="bottomRight">
                    <Button type="text" icon={<MoreOutlined />} />
                  </Dropdown>
                </div>
              </div>

              <div className="weibo-content" style={{ marginTop: 12, lineHeight: 1.6 }}>
                {weibo.content}
              </div>

              {weibo.images && weibo.images.length > 0 && (
                <div className="weibo-images" style={{ display: 'flex', gap: '8px', marginTop: 12, flexWrap: 'wrap' }}>
                  {weibo.images.map((img, index) => (
                    <img 
                      key={index} 
                      src={img} 
                      alt="" 
                      className="weibo-image"
                      style={{ width: '100px', height: '100px', objectFit: 'cover', borderRadius: '4px', cursor: 'pointer' }}
                    />
                  ))}
                </div>
              )}

              <div className="weibo-footer" style={{ display: 'flex', justifyContent: 'space-around', marginTop: 16, paddingTop: 16, borderTop: '1px solid #f0f0f0' }}>
                <div
                  className={`weibo-action ${weibo.isLiked ? 'liked' : ''}`}
                  onClick={() => handleLike(weibo.id, !!weibo.isLiked)}
                  style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', color: weibo.isLiked ? '#ff4d4f' : '#666' }}
                >
                  {weibo.isLiked ? <LikeFilled /> : <LikeOutlined />}
                  <span>{weibo.likeCount}</span>
                </div>
                <div 
                  className="weibo-action" 
                  onClick={() => navigate(`/weibo/${weibo.id}`)}
                  style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', color: '#666' }}
                >
                  <MessageOutlined />
                  <span>{weibo.commentCount}</span>
                </div>
                <div 
                  className="weibo-action"
                  style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px', color: '#666' }}
                >
                  <ShareAltOutlined />
                  <span>{weibo.repostCount}</span>
                </div>
              </div>
            </Card>
          ))
        )}
      </div>
    </div>
  );
};

export default Home;
