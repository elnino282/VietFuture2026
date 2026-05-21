import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Separator } from '@/components/ui/separator';
import { Sheet, SheetContent, SheetDescription, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Switch } from '@/components/ui/switch';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Textarea } from '@/components/ui/textarea';
import {
    Building2,
    CheckCircle,
    Download,
    Filter,
    Info,
    XCircle,
} from 'lucide-react';
import type { AccountStatus, Buyer, BuyerFormData, BuyerRole, KYCStatus } from '../types';

interface BuyerDetailDrawerProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    buyer: Buyer | null;
    formData: BuyerFormData;
    onFormDataChange: (data: BuyerFormData) => void;
    activeTab: string;
    onTabChange: (tab: string) => void;
    onSave: () => void;
    onKYCVerify: () => void;
    onKYCReject: () => void;
    getKYCBadge: (status: KYCStatus) => string;
    kycActionsSupported?: boolean;
}

export function BuyerDetailDrawer({
    open,
    onOpenChange,
    buyer,
    formData,
    onFormDataChange,
    activeTab,
    onTabChange,
    onSave,
    onKYCVerify,
    onKYCReject,
    getKYCBadge,
    kycActionsSupported = false,
}: BuyerDetailDrawerProps) {
    return (
        <Sheet open={open} onOpenChange={onOpenChange}>
            <SheetContent className="w-full sm:w-[700px] overflow-y-auto">
                <SheetHeader>
                    <SheetTitle className="flex items-center gap-2">
                        <Building2 className="w-5 h-5" />
                        {buyer ? 'Buyer Details' : 'Add New Buyer'}
                    </SheetTitle>
                    <SheetDescription>
                        {buyer ? `Manage ${buyer.companyName}` : 'Create a new buyer account'}
                    </SheetDescription>
                </SheetHeader>

                <Tabs value={activeTab} onValueChange={onTabChange} className="mt-6">
                    <TabsList className="grid w-full grid-cols-4">
                        <TabsTrigger value="general">General</TabsTrigger>
                        <TabsTrigger value="kyc" disabled={!buyer}>
                            KYC
                        </TabsTrigger>
                        <TabsTrigger value="permissions" disabled={!buyer}>
                            Permissions
                        </TabsTrigger>
                        <TabsTrigger value="history" disabled={!buyer}>
                            History
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent value="general" className="space-y-6 mt-6">
                        <div className="space-y-2">
                            <Label htmlFor="companyName">Company Name *</Label>
                            <Input
                                id="companyName"
                                placeholder="Enter company name"
                                value={formData.companyName}
                                onChange={(e) => onFormDataChange({ ...formData, companyName: e.target.value })}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="taxId">Tax/VAT ID *</Label>
                            <Input
                                id="taxId"
                                placeholder="VAT-123456789"
                                value={formData.taxId}
                                onChange={(e) => onFormDataChange({ ...formData, taxId: e.target.value })}
                            />
                        </div>

                        <Separator />

                        <div className="space-y-2">
                            <Label htmlFor="contactName">Contact Name *</Label>
                            <Input
                                id="contactName"
                                placeholder="Enter contact name"
                                value={formData.contactName}
                                onChange={(e) => onFormDataChange({ ...formData, contactName: e.target.value })}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="email">Email Address *</Label>
                            <Input
                                id="email"
                                type="email"
                                placeholder="buyer@example.com"
                                value={formData.email}
                                onChange={(e) => onFormDataChange({ ...formData, email: e.target.value })}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="phone">Phone Number *</Label>
                            <Input
                                id="phone"
                                type="tel"
                                placeholder="+1 234 567 8900"
                                value={formData.phone}
                                onChange={(e) => onFormDataChange({ ...formData, phone: e.target.value })}
                            />
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="address">Address</Label>
                            <Textarea
                                id="address"
                                placeholder="Enter company address"
                                value={formData.address}
                                onChange={(e) => onFormDataChange({ ...formData, address: e.target.value })}
                                rows={3}
                            />
                        </div>

                        <Separator />

                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-2">
                                <Label htmlFor="role">Role *</Label>
                                <Select
                                    value={formData.role}
                                    onValueChange={(v) => onFormDataChange({ ...formData, role: v as BuyerRole })}
                                >
                                    <SelectTrigger id="role">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="buyer">Buyer</SelectItem>
                                        <SelectItem value="enterprise">Enterprise</SelectItem>
                                        <SelectItem value="distributor">Distributor</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="accountStatus">Account Status *</Label>
                                <Select
                                    value={formData.accountStatus}
                                    onValueChange={(v) => onFormDataChange({ ...formData, accountStatus: v as AccountStatus })}
                                >
                                    <SelectTrigger id="accountStatus">
                                        <SelectValue />
                                    </SelectTrigger>
                                    <SelectContent>
                                        <SelectItem value="active">Active</SelectItem>
                                        <SelectItem value="suspended">Suspended</SelectItem>
                                        <SelectItem value="closed">Closed</SelectItem>
                                    </SelectContent>
                                </Select>
                            </div>
                        </div>

                        <div className="space-y-2">
                            <Label htmlFor="paymentTerms">Payment Terms</Label>
                            <Input
                                id="paymentTerms"
                                placeholder="e.g., Net 30, Cash on delivery"
                                value={formData.paymentTerms}
                                onChange={(e) => onFormDataChange({ ...formData, paymentTerms: e.target.value })}
                            />
                        </div>

                        <Separator />

                        <div className="flex gap-3">
                            <Button variant="outline" className="flex-1" onClick={() => onOpenChange(false)}>
                                Cancel
                            </Button>
                            <Button className="flex-1" onClick={onSave}>
                                {buyer ? 'Update Buyer' : 'Create Buyer'}
                            </Button>
                        </div>
                    </TabsContent>

                    <TabsContent value="kyc" className="space-y-6 mt-6">
                        {buyer && (
                            <>
                                <div className="flex items-center justify-between p-4 rounded-lg border bg-muted/30">
                                    <div>
                                        <p className="font-medium text-sm">Current KYC Status</p>
                                        <p className="text-xs text-muted-foreground mt-1">
                                            Verification status for this buyer
                                        </p>
                                    </div>
                                    <Badge variant="secondary" className={getKYCBadge(buyer.kycStatus)}>
                                        {buyer.kycStatus}
                                    </Badge>
                                </div>

                                <Separator />

                                <div>
                                    <div className="flex items-center justify-between mb-4">
                                        <h4>KYC Documents</h4>
                                    </div>
                                    <div className="rounded-lg border bg-muted/30 p-4 text-sm text-muted-foreground">
                                        <div className="flex items-center gap-2 text-foreground mb-2">
                                            <Info className="w-4 h-4" />
                                            KYC backend integration is not available yet.
                                        </div>
                                        <p>
                                            This tab is read-only for now. Verification/rejection actions are disabled until a dedicated KYC API is provided.
                                        </p>
                                    </div>
                                </div>

                                <Separator />

                                <div className="flex gap-3">
                                    <Button
                                        variant="outline"
                                        className="flex-1 text-red-600 hover:text-red-700"
                                        onClick={onKYCReject}
                                        disabled={!kycActionsSupported || buyer.kycStatus === 'rejected'}
                                    >
                                        <XCircle className="w-4 h-4 mr-2" />
                                        Reject KYC
                                    </Button>
                                    <Button
                                        className="flex-1 bg-emerald-600 hover:bg-emerald-700"
                                        onClick={onKYCVerify}
                                        disabled={!kycActionsSupported || buyer.kycStatus === 'verified'}
                                    >
                                        <CheckCircle className="w-4 h-4 mr-2" />
                                        Verify KYC
                                    </Button>
                                </div>
                            </>
                        )}
                    </TabsContent>

                    <TabsContent value="permissions" className="space-y-6 mt-6">
                        {buyer && (
                            <>
                                <div className="space-y-4">
                                    <div className="flex items-center justify-between p-4 rounded-lg border bg-muted/30">
                                        <div className="flex-1">
                                            <Label htmlFor="canPlaceOrders" className="cursor-pointer">
                                                Place Orders
                                            </Label>
                                            <p className="text-xs text-muted-foreground mt-1">
                                                Allow buyer to place purchase orders
                                            </p>
                                        </div>
                                        <Switch id="canPlaceOrders" defaultChecked />
                                    </div>

                                    <div className="flex items-center justify-between p-4 rounded-lg border bg-muted/30">
                                        <div className="flex-1">
                                            <Label htmlFor="canViewPricing" className="cursor-pointer">
                                                View Pricing
                                            </Label>
                                            <p className="text-xs text-muted-foreground mt-1">
                                                Allow buyer to view product pricing
                                            </p>
                                        </div>
                                        <Switch id="canViewPricing" defaultChecked />
                                    </div>

                                    <div className="flex items-center justify-between p-4 rounded-lg border bg-muted/30">
                                        <div className="flex-1">
                                            <Label htmlFor="canExportData" className="cursor-pointer">
                                                Export Data
                                            </Label>
                                            <p className="text-xs text-muted-foreground mt-1">
                                                Allow buyer to export reports and data
                                            </p>
                                        </div>
                                        <Switch id="canExportData" />
                                    </div>

                                    <div className="flex items-center justify-between p-4 rounded-lg border bg-muted/30">
                                        <div className="flex-1">
                                            <Label htmlFor="canViewReports" className="cursor-pointer">
                                                View Reports
                                            </Label>
                                            <p className="text-xs text-muted-foreground mt-1">
                                                Allow buyer to access analytics and reports
                                            </p>
                                        </div>
                                        <Switch id="canViewReports" defaultChecked />
                                    </div>
                                </div>

                                <Separator />

                                <div className="flex gap-3">
                                    <Button variant="outline" className="flex-1" onClick={() => onOpenChange(false)}>
                                        Cancel
                                    </Button>
                                    <Button className="flex-1" disabled>
                                        Save Permissions
                                    </Button>
                                </div>
                            </>
                        )}
                    </TabsContent>

                    <TabsContent value="history" className="space-y-4 mt-6">
                        {buyer && (
                            <>
                                <div className="flex items-center gap-2">
                                    <Button variant="outline" size="sm" className="flex-1">
                                        <Filter className="w-4 h-4 mr-2" />
                                        Filter
                                    </Button>
                                    <Button variant="outline" size="sm" className="flex-1">
                                        <Download className="w-4 h-4 mr-2" />
                                        Export
                                    </Button>
                                </div>

                                <div className="space-y-3">
                                    <div className="rounded-lg border bg-muted/30 p-4 text-sm text-muted-foreground">
                                        Audit history endpoint is not available for buyer management yet.
                                    </div>
                                </div>
                            </>
                        )}
                    </TabsContent>
                </Tabs>
            </SheetContent>
        </Sheet>
    );
}
