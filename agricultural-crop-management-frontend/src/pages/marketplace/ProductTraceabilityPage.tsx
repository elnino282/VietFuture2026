import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ProductTraceabilityView, TraceabilityProps } from '@/features/marketplace/components/ProductTraceabilityView';
import { Sprout, Leaf, Droplets, Tractor, ShieldAlert, ArrowLeft } from 'lucide-react';

export const ProductTraceabilityPage: React.FC = () => {
  const { slug } = useParams<{ slug: string }>(); // Using slug or productId
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [data, setData] = useState<TraceabilityProps | null>(null);

  useEffect(() => {
    // Simulating API Call
    const fetchTraceabilityData = (id: string) => {
      setLoading(true);
      setError(null);
      
      setTimeout(() => {
        // Mock error condition (e.g. invalid ID)
        if (id === 'invalid-id') {
          setError('Mã truy xuất không hợp lệ hoặc sản phẩm chưa cập nhật nhật ký.');
          setLoading(false);
          return;
        }

        // Mock success data
        setData({
          productInfo: {
            farmName: 'HTX Nông Nghiệp Xanh Đồng Tháp',
            season: 'Vụ Hè Thu 2026',
            name: 'Gạo Đài Thơm 8'
          },
          safetyMetrics: {
            water: {
              title: 'Nguồn nước sạch',
              description: 'Nước giếng khoan qua hệ thống lọc sinh học, không nhiễm phèn, kim loại nặng.',
              status: 'Đạt chuẩn an toàn'
            },
            fertilizer: {
              title: 'Phân bón & Phòng trừ',
              description: 'Sử dụng 100% phân bón hữu cơ vi sinh, không thuốc trừ sâu hóa học độc hại.',
              status: 'Đạt chuẩn hữu cơ'
            }
          },
          growthTimeline: [
            {
              id: '1',
              date: '12 Thg 03, 2026',
              action: 'Gieo hạt giống Đài Thơm 8',
              description: 'Gieo hạt giống thuần chủng tại lô đất L01. Đảm bảo mật độ gieo sạ chuẩn.',
              icon: Sprout,
              image: 'https://images.unsplash.com/photo-1592982537447-6f296a2d1d07?auto=format&fit=crop&q=80&w=600',
              isCompleted: true,
            },
            {
              id: '2',
              date: '25 Thg 03, 2026',
              action: 'Bón phân hữu cơ đợt 1',
              description: 'Sử dụng phân bón hữu cơ vi sinh, tăng cường dưỡng chất cho đất một cách tự nhiên.',
              icon: Leaf,
              image: 'https://images.unsplash.com/photo-1627920769213-9111b15170d1?auto=format&fit=crop&q=80&w=600',
              isCompleted: true,
            },
            {
              id: '3',
              date: '10 Thg 04, 2026',
              action: 'Kiểm tra chất lượng nước tưới',
              description: 'Nguồn nước tưới đạt chuẩn an toàn, không nhiễm phèn, đảm bảo cây sinh trưởng tốt.',
              icon: Droplets,
              isCompleted: true,
            },
            {
              id: '4',
              date: '20 Thg 06, 2026',
              action: 'Thu hoạch lúa chín',
              description: 'Thu hoạch vào lúc sáng sớm, sử dụng máy móc hiện đại giúp giữ nguyên vẹn chất lượng hạt gạo.',
              icon: Tractor,
              image: 'https://images.unsplash.com/photo-1595856403254-20b8f05e36f4?auto=format&fit=crop&q=80&w=600',
              isCompleted: true,
            }
          ]
        });
        setLoading(false);
      }, 1500); // 1.5 seconds delay
    };

    if (slug) {
      fetchTraceabilityData(slug);
    }
  }, [slug]);

  if (loading) {
    return (
      <div className="min-h-screen bg-neutral-50 pb-20 font-sans flex flex-col">
        <div className="bg-emerald-900 px-5 py-8 md:px-8 md:py-12 rounded-b-3xl shadow-lg relative h-64 animate-pulse"></div>
        <div className="max-w-3xl mx-auto px-4 -mt-6 relative z-20 w-full">
          <div className="bg-white rounded-2xl p-5 md:p-6 shadow-sm mb-8 h-40 animate-pulse"></div>
          <div className="bg-white rounded-2xl p-5 md:p-6 shadow-sm h-[500px] animate-pulse flex flex-col items-center justify-center">
             <div className="w-12 h-12 border-4 border-emerald-500 border-t-transparent rounded-full animate-spin mb-4" />
             <p className="text-emerald-700 font-medium">Đang tải dữ liệu truy xuất...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="min-h-screen bg-neutral-50 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl shadow-lg p-8 max-w-md w-full text-center border border-red-100">
          <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mx-auto mb-4">
            <ShieldAlert className="w-8 h-8 text-red-500" />
          </div>
          <h2 className="text-xl font-bold text-neutral-800 mb-2">Truy xuất thất bại</h2>
          <p className="text-neutral-600 mb-6">{error || 'Không tìm thấy dữ liệu.'}</p>
          <button 
            onClick={() => navigate('/marketplace')}
            className="inline-flex items-center justify-center gap-2 w-full px-6 py-3 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-medium transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            Quay lại cửa hàng
          </button>
        </div>
      </div>
    );
  }

  return <ProductTraceabilityView {...data} />;
};
