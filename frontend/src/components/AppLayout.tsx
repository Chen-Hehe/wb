import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Avatar, Dropdown, Space, Button, Modal } from 'antd';
import { HomeOutlined, UserOutlined, LogoutOutlined, LoginOutlined, UserAddOutlined } from '@ant-design/icons';
import type { MenuProps } from 'antd';
import { getCurrentUser, logout } from '../api/auth';

const { Header, Content, Footer } = Layout;

interface UserInfo {
  id: number;
  username: string;
  nickname: string;
  avatar?: string;
}

const AppLayout = ({ children }: { children: React.ReactNode }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const [user, setUser] = useState<UserInfo | null>(null);

  useEffect(() => {
    const userInfo = getCurrentUser();
    setUser(userInfo);
    
    // 监听存储变化（用于处理退出登录）
    const handleStorageChange = () => {
      const currentUser = getCurrentUser();
      setUser(currentUser);
    };
    
    window.addEventListener('storage', handleStorageChange);
    return () => window.removeEventListener('storage', handleStorageChange);
  }, []);

  const handleLogout = () => {
    Modal.confirm({
      title: '确认退出',
      content: '确定要退出登录吗？',
      okText: '确认',
      cancelText: '取消',
      onOk: () => {
        logout();
        setUser(null);
        navigate('/login');
      },
    });
  };

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: <Link to={`/user/${user?.id}`}>个人主页</Link>,
      onClick: (e) => e.domEvent.stopPropagation(),
    },
    {
      type: 'divider',
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: (e) => {
        e.domEvent.stopPropagation();
        handleLogout();
      },
    },
  ];

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'space-between',
        padding: '0 24px',
        background: '#fff',
        boxShadow: '0 2px 8px rgba(0,0,0,0.1)'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
          <Link to="/" style={{ fontSize: '20px', fontWeight: 'bold', color: '#1890ff', textDecoration: 'none' }}>
              微博
          </Link>
          <Menu
            theme="light"
            mode="horizontal"
            selectedKeys={[location.pathname]}
            style={{ flex: 1, minWidth: 0, border: 'none' }}
            items={[
              {
                key: '/',
                icon: <HomeOutlined />,
                label: <Link to="/">首页</Link>,
              },
            ]}
          />
        </div>
        
        <div>
          {user ? (
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <Space style={{ cursor: 'pointer' }}>
                <Avatar src={user.avatar} icon={<UserOutlined />} />
                <span style={{ color: '#333' }}>{user.nickname || user.username}</span>
              </Space>
            </Dropdown>
          ) : (
            <Space>
              <Button type="text" icon={<LoginOutlined />} onClick={() => navigate('/login')}>
                登录
              </Button>
              <Button type="primary" icon={<UserAddOutlined />} onClick={() => navigate('/register')}>
                注册
              </Button>
            </Space>
          )}
        </div>
      </Header>
      
      <Content style={{ padding: '24px', background: '#f0f2f5', minHeight: 'calc(100vh - 128px)' }}>
        <div style={{ background: '#fff', padding: '24px', borderRadius: '8px', minHeight: 'calc(100vh - 176px)' }}>
          {children}
        </div>
      </Content>
      
      <Footer style={{ textAlign: 'center', background: '#fff' }}>
        微博系统 ©2026 Created by cc
      </Footer>
    </Layout>
  );
};

export default AppLayout;
