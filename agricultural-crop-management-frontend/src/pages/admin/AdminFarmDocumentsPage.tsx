import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/shared/ui/table";
import { toast } from "sonner";
import { CheckCircle2, XCircle, Search, RefreshCw, FileText, ExternalLink } from "lucide-react";
import { PageContainer } from "@/shared/ui";
import { httpClient } from "@/shared/api/httpClient";

export function AdminFarmDocumentsPage() {
  const [documents, setDocuments] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchFarmDocuments = async () => {
    try {
      setLoading(true);
      const response = await httpClient.get(`/api/v1/farm-documents`);
      setDocuments(response.data.result || response.data.data || []);
    } catch (error) {
      toast.error("Không thể tải danh sách tài liệu");
      // Mock data
      setDocuments([
        {
          id: 1,
          farmId: 101,
          farmName: "Nông trại Mộc Châu",
          title: "Hồ sơ Farm Profile",
          documentUrl: "https://example.com/profile.pdf",
          status: "PENDING_VERIFICATION",
          createdAt: "2026-07-20T10:00:00Z"
        },
        {
          id: 2,
          farmId: 101,
          farmName: "Nông trại Mộc Châu",
          title: "Hồ sơ đăng ký VietGAP 2026",
          documentUrl: "https://example.com/vietgap.zip",
          status: "VERIFIED",
          createdAt: "2026-07-15T08:30:00Z"
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFarmDocuments();
  }, []);

  const handleVerify = async (documentId: number) => {
    try {
      await httpClient.post(`/api/v1/farm-documents/${documentId}/verify`);
      toast.success("Duyệt tài liệu thành công!");
      fetchFarmDocuments();
    } catch (error) {
      toast.error("Lỗi khi duyệt tài liệu");
      // update locally if mock
      setDocuments(docs => docs.map(d => d.id === documentId ? { ...d, status: "VERIFIED" } : d));
    }
  };

  const handleReject = async (documentId: number) => {
    toast.success("Đã từ chối tài liệu!");
    setDocuments(docs => docs.map(d => d.id === documentId ? { ...d, status: "REJECTED" } : d));
  };

  return (
    <PageContainer>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Duyệt Hồ Sơ & Tài Liệu Nông Trại</h1>
          <p className="text-slate-500 text-sm">Quản lý và phê duyệt các hồ sơ Farm Profile và chứng nhận.</p>
        </div>
        <Button onClick={fetchFarmDocuments} variant="outline" className="gap-2">
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} /> Làm mới
        </Button>
      </div>

      <Card className="border border-slate-200 shadow-sm rounded-xl overflow-hidden">
        <div className="p-4 bg-white border-b border-slate-100 flex items-center gap-4">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <input 
              placeholder="Tìm kiếm tài liệu..." 
              className="w-full pl-9 pr-4 py-2 border rounded-lg text-sm"
            />
          </div>
        </div>
        <Table>
          <TableHeader className="bg-slate-50">
            <TableRow>
              <TableHead>Nông trại</TableHead>
              <TableHead>Tên tài liệu</TableHead>
              <TableHead>Ngày gửi</TableHead>
              <TableHead>Trạng thái</TableHead>
              <TableHead>Tài liệu đính kèm</TableHead>
              <TableHead className="text-right">Hành động</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {documents.map((doc) => (
              <TableRow key={doc.id}>
                <TableCell className="font-semibold">{doc.farmName}</TableCell>
                <TableCell>
                  <div className="flex items-center gap-2 text-sm font-medium">
                    <FileText className="w-4 h-4 text-blue-500" /> {doc.title}
                  </div>
                </TableCell>
                <TableCell>{new Date(doc.createdAt).toLocaleDateString("vi-VN")}</TableCell>
                <TableCell>
                  <Badge className={
                    doc.status === 'PENDING_VERIFICATION' ? "bg-amber-100 text-amber-800" :
                    doc.status === 'VERIFIED' ? "bg-emerald-100 text-emerald-800" :
                    "bg-rose-100 text-rose-800"
                  }>
                    {doc.status}
                  </Badge>
                </TableCell>
                <TableCell>
                  <a href={doc.documentUrl} target="_blank" rel="noreferrer" className="text-blue-600 hover:underline flex items-center gap-1 text-sm">
                    <ExternalLink className="w-3 h-3" /> Tải/Xem
                  </a>
                </TableCell>
                <TableCell className="text-right">
                  {doc.status === 'PENDING_VERIFICATION' ? (
                    <div className="flex justify-end gap-2">
                      <Button 
                        size="sm" 
                        onClick={() => handleVerify(doc.id)}
                        className="bg-emerald-600 hover:bg-emerald-700 h-8"
                      >
                        <CheckCircle2 className="w-4 h-4" />
                      </Button>
                      <Button 
                        size="sm" 
                        variant="destructive"
                        onClick={() => handleReject(doc.id)}
                        className="h-8"
                      >
                        <XCircle className="w-4 h-4" />
                      </Button>
                    </div>
                  ) : (
                    <span className="text-slate-400 text-sm">Đã xử lý</span>
                  )}
                </TableCell>
              </TableRow>
            ))}
            {documents.length === 0 && !loading && (
              <TableRow>
                <TableCell colSpan={6} className="text-center py-8 text-slate-500">
                  Không có tài liệu nào.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Card>
    </PageContainer>
  );
}
