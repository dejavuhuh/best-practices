import { Chip, Link, Paper, Stack, Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from '@mui/material'
import Button from '@mui/material/Button'
import { useMutation, useQuery } from '@tanstack/react-query'
import dayjs from 'dayjs'
import wretch from 'wretch'
import { Progress } from './components/Progress'
import { TaskProgress } from './components/TaskProgress'
import 'dayjs/locale/zh-cn'

interface Task {
  id: number
  s3ObjectKey?: string
  downloadFileName: string
  state: 'RUNNING' | 'COMPLETED'
  finishedAt: string
}

export default function ButtonUsage() {
  const { data = [], refetch } = useQuery({
    queryKey: ['tasks'],
    queryFn: () => wretch('/api/export/tasks').get().json<Task[]>(),
  })

  const { mutate: mockHugeData, isPending: isMocking } = useMutation({
    mutationFn: () => wretch('/api/mock-huge-data').post({
      size: 1000000,
    }).json(),
  })

  const { mutate: startExport, isPending: isExporting } = useMutation({
    mutationFn: () => wretch('/api/export/start').post({
      downloadFileName: '海量数据',
    }).json(),
    onSuccess: () => refetch(),
    // eslint-disable-next-line no-alert
    onError: () => alert('暂无数据，请先生成数据'),
  })

  async function download(task: Task) {
    const url = await wretch(`/api/export/url?taskId=${task.id}`).get().text()
    const a = document.createElement('a')
    a.href = url
    a.click()
    a.remove()
  }

  return (
    <Stack direction="column" spacing={1}>
      <Stack direction="row" spacing={1}>
        <Button
          variant="contained"
          onClick={() => mockHugeData()}
          loading={isMocking}
          loadingPosition="start"
        >
          生成数据(100万)
        </Button>
        <Button
          variant="contained"
          onClick={() => startExport()}
          loading={isExporting}
          loadingPosition="start"
        >
          异步导出
        </Button>
      </Stack>
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell width={120}>任务ID</TableCell>
              <TableCell width={180}>文件名</TableCell>
              <TableCell width={180}>任务状态</TableCell>
              <TableCell width={320}>完成时间</TableCell>
              <TableCell align="right">当前进度(%)</TableCell>
              <TableCell align="right" width={120}>操作</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {data.map(row => (
              <TableRow
                key={row.id}
                sx={{ '&:last-child td, &:last-child th': { border: 0 } }}
              >
                <TableCell component="th" scope="row">
                  {row.id}
                </TableCell>
                <TableCell>{row.downloadFileName}</TableCell>
                <TableCell>
                  <Chip label={row.state} variant="filled" color={row.state === 'RUNNING' ? 'info' : 'success'} />
                </TableCell>
                <TableCell>{row.finishedAt ? dayjs(row.finishedAt).format('LLL') : null}</TableCell>
                <TableCell align="right">
                  {row.state === 'COMPLETED'
                    ? <Progress value={100} />
                    : <TaskProgress taskId={row.id} onCompleted={refetch} />}
                </TableCell>
                <TableCell align="right">
                  <Link underline="hover" component="button" onClick={() => download(row)}>下载</Link>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Stack>
  )
}
