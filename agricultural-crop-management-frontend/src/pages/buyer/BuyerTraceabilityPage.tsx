import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { MapPin, Search, ScanLine, Clock, ArrowRight } from "lucide-react";

export function BuyerTraceabilityPage() {
  const [traceCode, setTraceCode] = useState("");
  const navigate = useNavigate();

  const handleTrace = (e: React.FormEvent) => {
    e.preventDefault();
    if (traceCode.trim()) {
      navigate(`/trace/${traceCode.trim()}`);
    }
  };

  const recentTraces = [
    { code: "LOT-2026-001", name: "Dưa lưới chuẩn VietGAP", date: "2026-07-20" },
    { code: "LOT-2026-002", name: "Cà chua Cherry hữu cơ", date: "2026-07-19" },
  ];

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
          <MapPin className="w-6 h-6 text-emerald-600" />
          Truy xuất nguồn gốc
        </h1>
        <p className="text-slate-500 mt-2">
          Quét mã QR hoặc nhập mã lô hàng để xem chi tiết thông tin canh tác và thời gian cách ly (PHI).
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <Card className="border-emerald-100 shadow-lg shadow-emerald-50">
          <CardHeader className="bg-emerald-50/50 pb-4 border-b border-emerald-50">
            <CardTitle className="flex items-center gap-2 text-emerald-800">
              <Search className="w-5 h-5" /> Nhập mã truy xuất
            </CardTitle>
            <CardDescription>Nhập mã vạch hoặc mã lô hàng trên bao bì sản phẩm</CardDescription>
          </CardHeader>
          <CardContent className="pt-6">
            <form onSubmit={handleTrace} className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium text-slate-700">Mã lô hàng (Lot Code)</label>
                <div className="flex gap-2">
                  <Input 
                    placeholder="VD: LOT-2026-123" 
                    value={traceCode}
                    onChange={(e) => setTraceCode(e.target.value)}
                    className="flex-1"
                  />
                  <Button type="submit" className="bg-emerald-600 hover:bg-emerald-700">
                    Tra cứu
                  </Button>
                </div>
              </div>
              
              <div className="relative py-4">
                <div className="absolute inset-0 flex items-center">
                  <span className="w-full border-t border-slate-200" />
                </div>
                <div className="relative flex justify-center text-xs uppercase">
                  <span className="bg-white px-2 text-slate-500">Hoặc</span>
                </div>
              </div>

              <Button type="button" variant="outline" className="w-full gap-2 text-slate-600 hover:text-emerald-700 hover:bg-emerald-50 hover:border-emerald-200">
                <ScanLine className="w-4 h-4" /> Quét mã QR bằng Camera
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-slate-700">
              <Clock className="w-5 h-5" /> Lịch sử tra cứu
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentTraces.map((trace, i) => (
                <div key={i} className="flex items-center justify-between p-3 rounded-lg border border-slate-100 hover:bg-slate-50 transition-colors cursor-pointer" onClick={() => navigate(`/trace/${trace.code}`)}>
                  <div>
                    <h4 className="font-semibold text-slate-800 text-sm">{trace.name}</h4>
                    <p className="text-xs text-slate-500 mt-1 flex items-center gap-2">
                      <span className="font-mono bg-slate-100 px-1.5 rounded">{trace.code}</span>
                      <span>•</span>
                      <span>{trace.date}</span>
                    </p>
                  </div>
                  <ArrowRight className="w-4 h-4 text-slate-400" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
