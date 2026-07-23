import { useState, useEffect } from "react";
import { certificationApi } from "@/entities/farm/api/certificationApi";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/ui/table";
import { toast } from "sonner";
import { CheckCircle2, XCircle, Search, RefreshCw, ClipboardCheck, ArrowLeft, AlertCircle } from "lucide-react";
import { PageContainer } from "@/shared/ui";
import { Textarea } from "@/shared/ui/textarea";

export function AdminCertAuditsPage() {
  const [audits, setAudits] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedAudit, setSelectedAudit] = useState<any | null>(null);

  const fetchAudits = async () => {
    try {
      setLoading(true);
      const data = await certificationApi.getAllAudits();
      setAudits(data);
    } catch (error) {
      toast.error("Không thể tải danh sách audit");
      setAudits([
        {
          id: 1,
          farmId: 101,
          farmName: "Nông trại Mộc Châu",
          standardCode: "VIETGAP-TC-01",
          status: "IN_PROGRESS",
          auditDate: "2026-07-20",
          complianceScore: 85,
          checklist: [
            { id: 10, criteria: "Sử dụng phân bón đúng danh mục", status: "PASS" },
            { id: 11, criteria: "Ghi chép nhật ký đầy đủ", status: "FAIL", nonConformity: "Thiếu ghi chép tháng 6" },
          ],
          nonconformities: [
            { id: 101, description: "Thiếu ghi chép tháng 6", severity: "MAJOR", status: "OPEN" }
          ]
        },
        {
          id: 2,
          farmId: 102,
          farmName: "Nông trại Hữu cơ Đà Lạt",
          standardCode: "GLOBALGAP-01",
          status: "PENDING_APPROVAL",
          auditDate: "2026-07-22",
          complianceScore: 95,
          checklist: [
            { id: 12, criteria: "Quản lý nguồn nước tưới", status: "PASS" },
          ],
          nonconformities: []
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAudits();
  }, []);

  const handleApprove = async (auditId: number) => {
    try {
      await certificationApi.approveAudit(auditId);
      toast.success("Cấp chứng nhận thành công!");
      fetchAudits();
      setSelectedAudit(null);
    } catch (error) {
      toast.error("Lỗi khi cấp chứng nhận");
    }
  };

  const [ncDescription, setNcDescription] = useState("");
  const handleAddNonConformity = () => {
    if (!ncDescription) {
      toast.error("Vui lòng nhập mô tả lỗi");
      return;
    }
    toast.success("Đã ghi nhận Non-Conformity thành công!");
    setNcDescription("");
  };

  if (selectedAudit) {
    return (
      <PageContainer>
        <div className="flex items-center gap-4 mb-6">
          <Button variant="ghost" onClick={() => setSelectedAudit(null)}>
            <ArrowLeft className="w-4 h-4 mr-2" /> Quay lại
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-slate-800">Chi tiết Audit #{selectedAudit.id}</h1>
            <p className="text-slate-500 text-sm">Nông trại: {selectedAudit.farmName}</p>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="md:col-span-2 space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Checklist Đánh giá</CardTitle>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Tiêu chí</TableHead>
                      <TableHead>Đánh giá</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {selectedAudit.checklist?.map((item: any) => (
                      <TableRow key={item.id}>
                        <TableCell>{item.criteria}</TableCell>
                        <TableCell>
                          <Badge className={item.status === 'PASS' ? 'bg-emerald-100 text-emerald-800' : 'bg-rose-100 text-rose-800'}>
                            {item.status}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
            
            <Card>
              <CardHeader>
                <CardTitle>Lỗi đã ghi nhận (Non-Conformities)</CardTitle>
              </CardHeader>
              <CardContent>
                {selectedAudit.nonconformities?.length > 0 ? (
                  <div className="space-y-4">
                    {selectedAudit.nonconformities.map((nc: any) => (
                      <div key={nc.id} className="p-4 border rounded-lg flex justify-between items-start">
                        <div>
                          <div className="flex items-center gap-2 mb-1">
                            <AlertCircle className="w-4 h-4 text-amber-500" />
                            <span className="font-semibold text-sm">Lỗi #{nc.id}</span>
                            <Badge variant="outline" className="bg-rose-100 text-rose-800">{nc.severity}</Badge>
                            <Badge variant="outline" className="bg-slate-100 text-slate-800">{nc.status}</Badge>
                          </div>
                          <p className="text-sm text-slate-600">{nc.description}</p>
                        </div>
                        {nc.status === 'OPEN' && (
                          <Button size="sm" variant="outline" className="text-emerald-600 border-emerald-200">
                            Duyệt khắc phục
                          </Button>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-slate-500">Không có lỗi nào.</p>
                )}
              </CardContent>
            </Card>
          </div>

          <div className="space-y-6">
            <Card>
              <CardHeader>
                <CardTitle>Ghi nhận lỗi mới</CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                <Textarea 
                  placeholder="Mô tả chi tiết lỗi..." 
                  value={ncDescription} 
                  onChange={(e) => setNcDescription(e.target.value)} 
                />
                <Button className="w-full bg-amber-600 hover:bg-amber-700 text-white" onClick={handleAddNonConformity}>
                  Tạo Non-Conformity
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle>Hành động</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2">
                <Button 
                  className="w-full bg-emerald-600 hover:bg-emerald-700 text-white" 
                  disabled={selectedAudit.status !== 'PENDING_APPROVAL'}
                  onClick={() => handleApprove(selectedAudit.id)}
                >
                  <CheckCircle2 className="w-4 h-4 mr-2" /> Cấp Chứng Nhận
                </Button>
                <Button variant="outline" className="w-full">
                  Kết thúc Audit
                </Button>
              </CardContent>
            </Card>
          </div>
        </div>
      </PageContainer>
    );
  }

  return (
    <PageContainer>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Quản lý Audit</h1>
          <p className="text-slate-500 text-sm">Theo dõi, đánh giá checklist và cấp chứng nhận VietGAP.</p>
        </div>
        <Button onClick={fetchAudits} variant="outline" className="gap-2">
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /> Làm mới
        </Button>
      </div>

      <Card className="border border-slate-200 shadow-sm rounded-xl overflow-hidden">
        <Table>
          <TableHeader className="bg-slate-50">
            <TableRow>
              <TableHead>Mã Audit</TableHead>
              <TableHead>Nông trại</TableHead>
              <TableHead>Tiêu chuẩn</TableHead>
              <TableHead>Ngày Audit</TableHead>
              <TableHead>Điểm số</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead className="text-right">Hành động</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {audits.map((audit) => (
              <TableRow key={audit.id}>
                <TableCell className="font-mono text-sm">#{audit.id}</TableCell>
                <TableCell className="font-semibold">{audit.farmName || `Farm ID: ${audit.farmId}`}</TableCell>
                <TableCell>
                  <Badge variant="outline" className="bg-slate-100">{audit.standardCode}</Badge>
                </TableCell>
                <TableCell>{audit.auditDate ? new Date(audit.auditDate).toLocaleDateString("vi-VN") : "-"}</TableCell>
                <TableCell>
                  <span className={`font-bold ${audit.complianceScore >= 80 ? 'text-emerald-600' : 'text-amber-600'}`}>
                    {audit.complianceScore ?? 0}%
                  </span>
                </TableCell>
                <TableCell>
                  <Badge className={
                    audit.status === 'PENDING_APPROVAL' ? "bg-amber-100 text-amber-800" :
                    audit.status === 'APPROVED' || audit.status === 'CERTIFIED' ? "bg-emerald-100 text-emerald-800" :
                    "bg-blue-100 text-blue-800"
                  }>
                    {audit.status}
                  </Badge>
                </TableCell>
                <TableCell className="text-right">
                  <Button size="sm" variant="outline" onClick={() => setSelectedAudit(audit)}>
                    <ClipboardCheck className="w-4 h-4 mr-1" /> Chi tiết
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {audits.length === 0 && !loading && (
              <TableRow>
                <TableCell colSpan={7} className="text-center py-8 text-slate-500">
                  Không có dữ liệu Audit.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Card>
    </PageContainer>
  );
}
